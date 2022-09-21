package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class EditProfile : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var credential: AuthCredential
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"
    private var mobileNumberRegex = "^[89][0-9]{9}$"
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadUser()
        inputObserver()

        binding.btnEditProfileSave.setOnClickListener {
            validateCredentials()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadUser() {
        auth = Firebase.auth
        user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.tfEditProfileEmail.editText?.setText(user?.email)
            binding.tfEditProfileUsername.editText?.setText(user?.displayName)
            binding.tfEditProfileMobileNumber.editText?.setText(user?.phoneNumber)
        }
    }

    private fun inputObserver() {
        binding.tfEditProfileEmail.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfEditProfileEmail.error = getString(R.string.email_empty)
            else if (!emailRegex.toRegex().matches(text!!)) binding.tfEditProfileEmail.error = getString(R.string.email_invalid)
            else binding.tfEditProfileEmail.error = null
        }
        binding.tfEditProfileUsername.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfEditProfileUsername.error = getString(R.string.username_empty)
            else binding.tfEditProfileUsername.error = null
        }
        binding.tfEditProfileMobileNumber.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfEditProfileMobileNumber.error = getString(R.string.mobile_number_empty)
            else if (!mobileNumberRegex.toRegex().matches(text!!)) binding.tfEditProfileMobileNumber.error = getString(R.string.mobile_number_invalid)
            else binding.tfEditProfileMobileNumber.error = null
        }
    }

    private fun validateCredentials() {
        clearErrors()

        val email = binding.tfEditProfileEmail.editText?.text.toString().trim {it <= ' '}
        val username = binding.tfEditProfileUsername.editText?.text.toString().trim {it <= ' '}
        val mobileNumber = binding.tfEditProfileMobileNumber.editText?.text.toString().trim {it <= ' '}

        if (emailRegex.toRegex().matches(email) && !TextUtils.isEmpty(username) && mobileNumberRegex.toRegex().matches(mobileNumber)) {
            disableWindow()
            usernameExists(email, username, mobileNumber)
        }
        else {
            if (!emailRegex.toRegex().matches(email)) binding.tfEditProfileEmail.error = getString(R.string.email_invalid)
            if (!mobileNumberRegex.toRegex().matches(mobileNumber)) binding.tfEditProfileMobileNumber.error = getString(R.string.mobile_number_invalid)
            if (TextUtils.isEmpty(email)) binding.tfEditProfileEmail.error = getString(R.string.email_empty)
            if (TextUtils.isEmpty(username)) binding.tfEditProfileUsername.error = getString(R.string.username_empty)
            if (TextUtils.isEmpty(mobileNumber)) binding.tfEditProfileMobileNumber.error = getString(R.string.mobile_number_empty)
        }
    }

    private fun usernameExists(email: String, username: String, mobileNumber: String) {
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

                if (!usernameKey) updateDetails(email, username, mobileNumber)
                else enableWindow()
            }

            override fun onCancelled(error: DatabaseError) {
                enableWindow()
                Log.e("databaseReference", error.toString())
            }
        })
    }

    private fun reauthenticateUser(email: String, password: String, username: String, mobileNumber: String) {
        credential = EmailAuthProvider.getCredential(user?.email.toString(), password)
        user!!.reauthenticate(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                updateDetails(email, username, mobileNumber)
            }
            else {
                enableWindow()
                Log.d("updatePassword", authTask.exception!!.message.toString())
            }
        }
    }

    private fun updateDetails(email: String, username: String, mobileNumber: String) {
        val phoneNumber = user?.phoneNumber.toString()
        if (TextUtils.isEmpty(phoneNumber)) {
//            auth.signInWithCredential()
        }
        else {
            enableWindow()
            Toast.makeText(this, phoneNumber, Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableWindow() {
        binding.btnEditProfileSave.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.btnEditProfileCancel.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.blue_cancel)
        binding.pbEditProfile.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnEditProfileSave.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.btnEditProfileCancel.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbEditProfile.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tfEditProfileEmail.error = null
        binding.tfEditProfileUsername.error = null
        binding.tfEditProfileMobileNumber.error = null
    }
}