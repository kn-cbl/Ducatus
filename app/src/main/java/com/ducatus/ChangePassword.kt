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
import com.ducatus.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChangePassword : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credential: AuthCredential
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbChangePassword.setNavigationOnClickListener {
            startActivity(Intent(this, Settings::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.btnChangePassword.setOnClickListener {
            // validate password -> reauthenticate user -> update password
            validatePassword()
        }

        binding.btnChangePasswordCancel.setOnClickListener {
            cancel()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun validatePassword() {
        val currentPassword = binding.etChangePasswordCurrent.text.toString().trim {it <= ' '}
        val newPassword = binding.etChangePasswordNew.text.toString().trim {it <= ' '}
        val confirmPassword = binding.etChangePasswordConfirm.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            if (TextUtils.isEmpty(currentPassword)) binding.tvChangePasswordCurrentError.text = "Please enter current password"
            if (TextUtils.isEmpty(newPassword)) binding.tvChangePasswordNewError.text = "Please enter new password"
            if (TextUtils.isEmpty(confirmPassword)) binding.tvChangePasswordConfirmError.text = "Please confirm new password"
        }
        else {
            if (newPassword != confirmPassword) {
                binding.tvChangePasswordConfirmError.text = "Passwords do not match"
            }
            else {
                disableWindow()
                reauthenticateUser(newPassword, currentPassword)
            }
        }
    }

    private fun reauthenticateUser(newPassword: String, currentPassword: String) {
        auth = Firebase.auth
        val user = FirebaseAuth.getInstance().currentUser
        credential = EmailAuthProvider.getCredential(user!!.email.toString(), currentPassword)
        user.reauthenticate(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                updatePassword(user, newPassword)
            }
            else {
                enableWindow()
                Log.d("updatePassword", authTask.exception!!.message.toString())
                binding.tvChangePasswordErrorAuth.text = authTask.exception!!.message
            }
        }
    }

    private fun updatePassword(user: FirebaseUser, newPassword: String) {
        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
            if (updateTask.isSuccessful) {
                crypto = Crypto()
                database = Firebase.database
                databaseReference = database.getReference("users/" + user.uid + "/password")
                databaseReference.setValue(crypto.encrypt(newPassword).toString())

                enableWindow()
                Toast.makeText(this, "Successfully changed password", Toast.LENGTH_SHORT).show()
            } else {
                enableWindow()
                Log.d("updatePassword", updateTask.exception!!.message.toString())
                binding.tvChangePasswordErrorAuth.text = updateTask.exception!!.message
            }
        }
    }

    private fun cancel() {
        startActivity(Intent(this, Settings::class.java))
    }

    private fun enableWindow() {
        binding.btnChangePassword.setBackgroundResource(R.drawable.green_button)
        binding.btnChangePasswordCancel.setBackgroundResource(R.drawable.cancel_button)
        binding.pbChangePassword.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnChangePassword.setBackgroundResource(R.drawable.btn_disabled)
        binding.btnChangePasswordCancel.setBackgroundResource(R.drawable.btn_disabled)
        binding.pbChangePassword.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}