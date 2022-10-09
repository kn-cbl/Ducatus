package com.ducatus

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUserProfileBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class UserProfileFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var currentUsername: String
    private val requestPickImage = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        toolbar = activity.findViewById(R.id.tbUserProfile)
        toolbar.title = getString(R.string.user_profile)
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootLayout = activity.findViewById(R.id.llUserProfile)
        loadData()

//        binding.fragmentUserProfile.setOnClickListener {
//            // hide keyboard
//            val keyboard: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            keyboard.hideSoftInputFromWindow(binding.etUserProfileUsername.windowToken, 0)
//
//            // clear focus on textview
//            binding.etUserProfileUsername.clearFocus()
//            binding.etUserProfileUsername.isFocusable = false
//            binding.etUserProfileUsername.isFocusableInTouchMode = false
//        }

        binding.ivUserProfilePictureUpdate.setOnClickListener {
            selectImage()
        }

        binding.ibUpdateUsername.setOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateUsernameFragment(currentUsername)
            findNavController().navigate(action)
        }

//        binding.ibUpdateUsername.setOnClickListener {
//            // set textview as focusable and focus on the end of the text
//            binding.etUserProfileUsername.isFocusable = true
//            binding.etUserProfileUsername.isFocusableInTouchMode = true
//            binding.etUserProfileUsername.requestFocus()
//            binding.etUserProfileUsername.setSelection(binding.etUserProfileUsername.length())
//
//            // show keyboard
//            val keyboard: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            keyboard.showSoftInput(binding.etUserProfileUsername, 0)
//        }

        binding.tfUserProfileEmail.setEndIconOnClickListener {
            toolbar.title = getString(R.string.email)
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateEmailFragment()
            findNavController().navigate(action)
        }

        binding.tfUserProfileMobileNumber.setEndIconOnClickListener {
            toolbar.title = getString(R.string.mobile_number)
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateMobileNumberFragment()
            findNavController().navigate(action)
        }

//        binding.btnUserProfileSave.setOnClickListener {
//            // check changes -> update profile accordingly
//            validateChanges()
//        }
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            currentUsername = firebaseUser.displayName.toString()
            if (firebaseUser.photoUrl != null) {
                Picasso.get()
                    .load(firebaseUser.photoUrl)
                    .into(binding.ivUserProfilePicture)
            }

            val style = DateFormat.MEDIUM
            binding.tvJoinDate.text = DateFormat.getDateInstance(style, Locale.US).format(Date(firebaseUser.metadata!!.creationTimestamp))
            binding.etUserProfileUsername.setText(firebaseUser.displayName)
            binding.tfUserProfileEmail.editText?.setText(firebaseUser.email)
            if (firebaseUser.phoneNumber?.isNotEmpty() == true) binding.tfUserProfileMobileNumber.editText?.setText(firebaseUser.phoneNumber)

            hideProgressDialog()
        }
        else {
            sessionExpired()
        }
    }

    private fun selectImage() {
//        val photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
//        photoPickerIntent.putExtra("crop", "true")
//        photoPickerIntent.putExtra("aspectX", 1)
//        photoPickerIntent.putExtra("aspectY", 1)
//        photoPickerIntent.putExtra("outputX", 240)
//        photoPickerIntent.putExtra("outputY", 240)
//        photoPickerIntent.putExtra("return-data", true)
        startActivityForResult(photoPickerIntent, requestPickImage)

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == requestPickImage && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                val options = UCrop.Options()
                options.setHideBottomControls(false)
                options.setFreeStyleCropEnabled(true)

                val fragment = parentFragmentManager.findFragmentById(R.id.fcUserProfile)
                if (fragment != null) {
                    UCrop.of(uri, Uri.fromFile(File(activity.cacheDir, "sample_cropped_image.jpg")))
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(160, 160)
                        .withOptions(options)
                        .start(activity, fragment)
                }

            }

//            val photoUri = data?.data
//            Toast.makeText(activity, photoUri.toString(), Toast.LENGTH_SHORT).show()
//            if (photoUri != null) {
//                Toast.makeText(activity, "not null", Toast.LENGTH_SHORT).show()
////                updateProfile(photoUri)
//            }
//            else {
//                Toast.makeText(activity, "null", Toast.LENGTH_SHORT).show()
//            }
        }
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            if (data != null) {
                val resultUri = UCrop.getOutput(data)
                updateProfile(resultUri)
            }
        }
        else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val error = UCrop.getError(data)
                Snackbar
                    .make(rootLayout, error.toString(), Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun updateProfile(photoUri: Uri?) {
        showProgressDialog()
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val updates = UserProfileChangeRequest.Builder()
                .setDisplayName(firebaseUser.displayName)
                .setPhotoUri(photoUri)
                .build()

            firebaseUser.updateProfile(updates)
                .addOnSuccessListener {
                    Snackbar
                        .make(rootLayout, "Successfully updated picture", Snackbar.LENGTH_LONG)
                        .show()

                    loadData()
                }
                .addOnFailureListener {
                    Snackbar
                        .make(rootLayout, "Unable to save changes, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { updateProfile(photoUri) }
                        .show()
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbUserProfile.visibility = View.VISIBLE
        binding.llUserProfileFragment.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbUserProfile.visibility = View.INVISIBLE
        binding.llUserProfileFragment.visibility = View.VISIBLE
    }

//    private fun showProgressDialog() {
//        binding.pbUserProfile.visibility = View.VISIBLE
//        binding.btnUserProfileSave.text = null
//        binding.btnUserProfileSave.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.gray)
//        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
//    }
//
//    private fun hideProgressDialog() {
//        binding.pbUserProfile.visibility = View.INVISIBLE
//        binding.btnUserProfileSave.text = getString(R.string.save_changes)
//        binding.btnUserProfileSave.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
//        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
//    }
}