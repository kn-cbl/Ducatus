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
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUserProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class UserProfileFragment : Fragment(), DialogInterface.OnDismissListener {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var firebaseUser: FirebaseUser
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
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateUsernameDialogFragment(currentUsername)
            findNavController().navigate(action)
        }

        binding.tfUserProfileEmail.setEndIconOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateEmailDialogFragment()
            findNavController().navigate(action)
        }

        binding.tfUserProfileMobileNumber.setEndIconOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateMobileNumberDialogFragment()
            findNavController().navigate(action)
        }
    }

    override fun onResume() {
        super.onResume()

        var firebaseUser: FirebaseUser? = auth.currentUser
        firebaseUser?.reload()?.addOnCompleteListener {
            firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                Snackbar
                    .make(rootLayout, "Please log-in again", Snackbar.LENGTH_LONG)
                    .show()

                sessionExpired()
            }
        }
    }

    override fun onDismiss(p0: DialogInterface?) {
        loadData()
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!
            currentUsername = firebaseUser.displayName.toString()
            if (firebaseUser.photoUrl != null) {
                Picasso.get()
                    .load(firebaseUser.photoUrl)
                    .transform(CropCircleTransformation())
                    .into(binding.ivUserProfilePicture)
            }

            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(firebaseUser.metadata!!.creationTimestamp),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)
            binding.tvJoinDate.text = formattedDate

            binding.tvUserProfileUsername.text = firebaseUser.displayName
            binding.tfUserProfileEmail.editText?.setText(firebaseUser.email)
            if (firebaseUser.phoneNumber?.isNotEmpty() == true) binding.tfUserProfileMobileNumber.editText?.setText(firebaseUser.phoneNumber)

            isGoogleOnly(firebaseUser)
            hideProgressDialog()
        }
        else {
            Snackbar
                .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
                .show()

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
            binding.ibUpdateUsername.visibility = View.GONE
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
                    .make(rootLayout, error.toString(), 5000)
                    .show()
            }
        }
    }

    private fun updateProfile(photoUri: Uri?) {
        showProgressDialog()
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
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity.finish()
        }

        dialog.show()
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