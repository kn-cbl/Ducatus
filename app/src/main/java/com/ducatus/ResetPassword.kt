package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPassword : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        btnResetPassword.setOnClickListener {
            validatePassword()
        }
    }

    private fun validatePassword() {
        tvResetPasswordNewError.text = ""
        tvResetPasswordConfirmError.text = ""
        tvResetPasswordError.text = ""

        val newPassword = etResetPasswordNew.text.toString().trim {it <= ' '}
        val confirmPassword = etResetPasswordConfirm.text.toString().trim {it <= ' '}

        when {
            TextUtils.isEmpty(newPassword) -> {
                pbResetPassword.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvResetPasswordNewError.text = "Please enter new password"
            }

            TextUtils.isEmpty(confirmPassword) -> {
                pbResetPassword.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvResetPasswordConfirmError.text = "Please confirm new password"
            }

            newPassword != confirmPassword -> {
                pbResetPassword.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvResetPasswordError.text = "Passwords do not match"
            }

            else -> {
                pbResetPassword.visibility = View.VISIBLE
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
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Login::class.java))
                finish()
            }
            else {
                Log.e("resetPassword", task.exception!!.message.toString())
                tvResetPasswordError.text = task.exception!!.message
            }
        }
    }
}