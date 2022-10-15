package com.ducatus

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUserProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import java.io.File
import java.text.DateFormat
import java.util.*

class UserProfileFragment : Fragment(), DialogInterface.OnDismissListener {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var rootLayout: LinearLayout
    private lateinit var currentUsername: String
    private val requestPickImage = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llUserProfile)

        binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.ivUserProfilePictureUpdate.setOnClickListener {
            selectImage()
        }

        binding.ibUpdateUsername.setOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateUsernameFragment(currentUsername)
            findNavController().navigate(action)
        }

        binding.tfUserProfileEmail.setEndIconOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateEmailFragment()
            findNavController().navigate(action)
        }

        binding.tfUserProfileMobileNumber.setEndIconOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateMobileNumberFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        loadData()
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
                    .transform(CropCircleTransformation())
                    .into(binding.ivUserProfilePicture)
            }

            binding.tvJoinDate.text = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(firebaseUser.metadata!!.creationTimestamp))
            binding.tvUserProfileUsername.text = firebaseUser.displayName
            binding.tfUserProfileEmail.editText?.setText(firebaseUser.email)
            if (firebaseUser.phoneNumber?.isNotEmpty() == true) binding.tfUserProfileMobileNumber.editText?.setText(firebaseUser.phoneNumber)

            isGoogleOnly(firebaseUser)
            hideProgressDialog()
        }
        else {
            sessionExpired()
        }
    }

    private fun isGoogleOnly(firebaseUser: FirebaseUser) {
        val providers = mutableListOf<String>()
        for (item in firebaseUser.providerData) {
            providers.add(item.providerId)
        }

        if (!providers.contains("password")) {
            binding.tfUserProfileEmail.endIconMode = END_ICON_NONE
        }
    }

    private fun selectImage() {
//        val photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
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
                    UCrop.of(uri, Uri.fromFile(File(activity.cacheDir, "cropped_image.jpg")))
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(160, 160)
                        .withOptions(options)
                        .start(activity, fragment)
                }
            }
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
}