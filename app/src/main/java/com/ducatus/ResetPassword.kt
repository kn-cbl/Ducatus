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

        inputObserver()

        binding.btnResetPassword.setOnClickListener {
            validatePassword()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        auth = Firebase.auth
        auth.signOut()
    }

    private fun inputObserver() {
        binding.tfResetPasswordNew.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfResetPasswordNew.error = getString(R.string.new_password_empty)
            else  binding.tfResetPasswordNew.error = null
        }
        binding.tfResetPasswordConfirm.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfResetPasswordConfirm.error = getString(R.string.confirm_password_empty)
            else  binding.tfResetPasswordConfirm.error = null
        }
    }

    private fun validatePassword() {
        binding.tvResetPasswordError.text = ""
        binding.tfResetPasswordNew.error = null
        binding.tfResetPasswordConfirm.error = null

        val newPassword = binding.tfResetPasswordNew.editText?.text.toString().trim {it <= ' '}
        val confirmPassword = binding.tfResetPasswordConfirm.editText?.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            if (TextUtils.isEmpty(newPassword)) binding.tfResetPasswordNew.error = getString(R.string.new_password_empty)
            if (TextUtils.isEmpty(confirmPassword)) binding.tfResetPasswordConfirm.error = getString(R.string.confirm_password_empty)
        }
        else {
            if (newPassword != confirmPassword) {
                binding.tfResetPasswordConfirm.error = getString(R.string.password_match_error)
            }
            else {
                disableWindow()
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
                enableWindow()
                Log.e("resetPassword", task.exception!!.message.toString())
                binding.tvResetPasswordError.text = task.exception!!.message
            }
        }
    }

    private fun enableWindow() {
        binding.btnResetPassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbResetPassword.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnResetPassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbResetPassword.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}