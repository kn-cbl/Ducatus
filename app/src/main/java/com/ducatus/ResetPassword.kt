package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.ducatus.databinding.ActivityResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ResetPassword : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnResetPassword.setOnClickListener {
            validatePassword()
        }
    }

    private fun validatePassword() {
        binding.tvResetPasswordNewError.text = ""
        binding.tvResetPasswordConfirmError.text = ""
        binding.tvResetPasswordError.text = ""

        val newPassword = binding.etResetPasswordNew.text.toString().trim {it <= ' '}
        val confirmPassword = binding.etResetPasswordConfirm.text.toString().trim {it <= ' '}

        when {
            TextUtils.isEmpty(newPassword) -> {
                binding.pbResetPassword.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                binding.tvResetPasswordNewError.text = "Please enter new password"
            }

            TextUtils.isEmpty(confirmPassword) -> {
                binding.pbResetPassword.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                binding.tvResetPasswordConfirmError.text = "Please confirm new password"
            }

            newPassword != confirmPassword -> {
                binding.pbResetPassword.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                binding.tvResetPasswordError.text = "Passwords do not match"
            }

            else -> {
                binding.pbResetPassword.visibility = View.VISIBLE
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                resetPassword(newPassword)
            }
        }
    }

    private fun resetPassword(newPassword: String) {
        auth = Firebase.auth

        val firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        firebaseUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                crypto = Crypto()
                database = Firebase.database
                databaseReference = database.getReference("users/" + firebaseUser.uid + "/password")
                databaseReference.setValue(crypto.encrypt(newPassword).toString())

                Toast.makeText(this, "Successfully reset password", Toast.LENGTH_SHORT).show()
                auth.signOut()
                startActivity(Intent(this, Login::class.java))
                finish()
            }
            else {
                Log.e("resetPassword", task.exception!!.message.toString())
                binding.tvResetPasswordError.text = task.exception!!.message
            }
        }
    }
}