package com.ducatus

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivityResetPasswordBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

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
                showProgressDialog()
                resetPassword(newPassword)
            }
        }
    }

    private fun resetPassword(newPassword: String) {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.updatePassword(newPassword)
                .addOnSuccessListener {
                    crypto = Crypto()
                    database = Firebase.database
                    databaseReference = database.getReference("users/" + firebaseUser.uid + "/password")
                    databaseReference.setValue(crypto.encrypt(newPassword).toString())
                    auth.signOut()

                    Snackbar
                        .make(binding.llResetPassword, "Successfully reset password", Snackbar.LENGTH_LONG)
                        .show()

                    // add 3 second delay
                    object : CountDownTimer(3000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            // do nothing
                        }
                        override fun onFinish() {
                            val intent = Intent(applicationContext, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finish()
                        }
                    }.start()

                }
                .addOnFailureListener {
                    hideProgressDialog()
                    binding.tvResetPasswordError.text = it.localizedMessage
                }
        }
        else {
            val intent = Intent(this, ResetPasswordEmailActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

    }

    private fun showProgressDialog() {
        binding.btnResetPassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbResetPassword.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnResetPassword.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbResetPassword.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}