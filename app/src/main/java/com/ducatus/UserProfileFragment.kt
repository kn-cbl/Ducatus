package com.ducatus

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUserProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date

class UserProfileFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var currentUsername: String
    private var currentPhotoUri: Uri? = null
    private var photoUri: Uri? = null
    private val requestPickImage = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootLayout = activity.findViewById(R.id.clUserProfile)
        loadUser()

        binding.fragmentUserProfile.setOnClickListener {
            val keyboard: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.hideSoftInputFromWindow(binding.etUserProfileUsername.windowToken, 0)
            binding.etUserProfileUsername.clearFocus()
            binding.etUserProfileUsername.isFocusable = false
            binding.etUserProfileUsername.isFocusableInTouchMode = false
        }

        binding.imgViewUserProfilePictureUpdate.setOnClickListener {
            selectImage()
        }

        binding.imgViewUpdateUsername.setOnClickListener {
            binding.etUserProfileUsername.isFocusable = true
            binding.etUserProfileUsername.isFocusableInTouchMode = true
            binding.etUserProfileUsername.requestFocus()
            binding.etUserProfileUsername.setSelection(binding.etUserProfileUsername.length())
            val keyboard: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.showSoftInput(binding.etUserProfileUsername, 0)
        }

        binding.tfUserProfileEmail.setEndIconOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateEmailFragment()
            findNavController().navigate(action)
        }

        binding.tfUserProfileMobileNumber.setEndIconOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateMobileNumberFragment()
            findNavController().navigate(action)
        }

        binding.btnUserProfileSave.setOnClickListener {
            // check changes -> update profile accordingly
            validateChanges()
        }
    }

    private fun loadUser() {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            currentUsername = firebaseUser.displayName.toString()
            if (firebaseUser.photoUrl != null) {
                currentPhotoUri = firebaseUser.photoUrl
            }

            binding.imgViewUserProfilePicture.setImageURI(photoUri)
            binding.etUserProfileUsername.setText(firebaseUser.displayName)
            binding.tvJoinDate.text = SimpleDateFormat("MM/dd/yyyy").format(Date(firebaseUser.metadata!!.creationTimestamp))
            binding.tfUserProfileEmail.editText?.setText(firebaseUser.email)
            if (firebaseUser.phoneNumber?.isNotEmpty() == true) binding.tfUserProfileMobileNumber.editText?.setText(firebaseUser.phoneNumber)
        }
        else {
            sessionExpired()
        }
    }

    private fun selectImage() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        photoPickerIntent.putExtra("crop", "true")
        photoPickerIntent.putExtra("aspectX", 1)
        photoPickerIntent.putExtra("aspectY", 1)
        photoPickerIntent.putExtra("outputX", 240)
        photoPickerIntent.putExtra("outputY", 240)
        photoPickerIntent.putExtra("return-data", true)
        startActivityForResult(photoPickerIntent, requestPickImage)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestPickImage && resultCode == RESULT_OK) {
            val extras = data?.extras
            val bitmap = extras?.getParcelable<Bitmap>("data")

            binding.imgViewUserProfilePicture.setImageBitmap(bitmap)

            val photoUri = data?.data
            if (photoUri != null) {
//                updateProfile(photoUri)
            }
            else {
                Toast.makeText(activity, "null", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateChanges() {
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val username = binding.etUserProfileUsername.text.toString().trim {it <= ' '}

            // filtering changes is necessary to skip username validation if username was not changed
            when {
                // refresh fragment if no changes were made
                currentUsername == username && photoUri == null -> activity.onBackPressed()

                // username changed, none to photo
                currentUsername != username && photoUri == null -> usernameExists(firebaseUser.uid, username, currentPhotoUri)

                // photo changed, none to username
                currentUsername == username && photoUri != null -> usernameExists(firebaseUser.uid, username, photoUri)

                // username and photo changed
                currentUsername != username && photoUri != null -> usernameExists(firebaseUser.uid, username, photoUri)

                // username field is empty
                TextUtils.isEmpty(username) -> Snackbar.make(rootLayout, getString(R.string.username_empty), Snackbar.LENGTH_LONG).show()
            }
        }
        else {
            sessionExpired()
        }
    }

    private fun usernameExists(uid: String, username: String, photoUri: Uri?) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var usernameKey = false

                for (child in snapshot.children) {
                    if(username == child.child("username").value.toString()) {
                        usernameKey = true
                        break
                    }
                }

                if (!usernameKey) {
                    updateDB(uid, username, photoUri)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, getString(R.string.username_exists), Snackbar.LENGTH_LONG)
                        .show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, error.message, Snackbar.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun updateDB(uid: String, username: String, photoUri: Uri?) {
        databaseReference = database.getReference("users/" + uid + "/username")
        databaseReference.setValue(username)
            .addOnSuccessListener {
                updateProfile(username, photoUri)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Failed to update username", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { updateDB(uid, username, photoUri) }
                    .show()
            }
    }

    private fun updateProfile(username: String, photoUri: Uri?) {
        showProgressDialog()
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val updates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .setPhotoUri(photoUri)
                .build()

            firebaseUser.updateProfile(updates)
                .addOnSuccessListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "Successfully saved changes", Snackbar.LENGTH_LONG)
                        .show()
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "Failed to save changes", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") {updateProfile(username, photoUri)}
                        .show()
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun sessionExpired() {
        hideProgressDialog()
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        activity.finish()
    }

    private fun showProgressDialog() {
        binding.btnUserProfileSave.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.light_gray_text)
        binding.flUserProfile.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnUserProfileSave.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        binding.flUserProfile.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}