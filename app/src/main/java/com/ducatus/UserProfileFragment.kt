package com.ducatus

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUserProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date

class UserProfileFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
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
        loadUser()

        binding.imgViewUserProfilePictureUpdate.setOnClickListener {
            selectImage()
        }

        binding.imgViewUpdateUsername.setOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToUpdateUsernameFragment()
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

    private fun loadUser() {
        auth = Firebase.auth
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
//            binding.imgViewUserProfilePicture.setImageURI(authUser.photoUrl)
            binding.tvUserProfileUsername.text = authUser.displayName
            binding.tvJoinDate.text = SimpleDateFormat("MM/dd/yyyy").format(Date(authUser.metadata!!.creationTimestamp))
            binding.tfUserProfileEmail.editText?.setText(authUser.email)
            binding.tfUserProfileMobileNumber.editText?.setText(authUser.phoneNumber)
        }
        else {
            gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            gsc = GoogleSignIn.getClient(requireActivity(), gso)
            val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (googleSignInAccount != null) {
                binding.imgViewUserProfilePicture.setImageURI(googleSignInAccount.photoUrl)
                binding.imgViewUpdateUsername.visibility = View.INVISIBLE
                binding.imgViewUpdateUsername.isEnabled = false
                binding.tfUserProfileEmail.endIconDrawable = null

//                SimpleDateFormat("MM/dd/yyyy").format(Date(googleSignInAccount.))
                binding.tvUserProfileUsername.text = googleSignInAccount.displayName
                binding.tfUserProfileEmail.editText?.setText(googleSignInAccount.email)
            }
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
                updateProfile(photoUri)
            }
            else {
                Toast.makeText(activity, "null", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfile(photoUri: Uri) {
        val authUser = FirebaseAuth.getInstance().currentUser
        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(authUser?.displayName)
            .setPhotoUri(photoUri)
            .build()

        authUser?.updateProfile(updates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(activity, "success", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(activity, "failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}