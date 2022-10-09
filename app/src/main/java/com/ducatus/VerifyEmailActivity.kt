package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.ducatus.databinding.ActivityVerifyEmailBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class VerifyEmailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityVerifyEmailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            if (!firebaseUser.isEmailVerified) {
                sendEmail(firebaseUser)
            }
        }
        else {
            sessionExpired()
        }

        binding.btnResendEmail.setOnClickListener {
            if (firebaseUser != null) {
                resendEmail(firebaseUser)
            }
            else {
                sessionExpired()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reloadUser()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun sendEmail(firebaseUser: FirebaseUser) {
        firebaseUser.sendEmailVerification()
            .addOnFailureListener {
                binding.tvVerifyEmailError.text = it.localizedMessage
            }
        hideProgressDialog()
    }

    // reload email verified status of user
    private fun reloadUser() {
        binding.pbVerifyEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        var firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.reload()
                .addOnSuccessListener {
                    firebaseUser = auth.currentUser
                    isEmailVerified(firebaseUser!!)
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    binding.tvVerifyEmailError.text = it.localizedMessage
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun resendEmail(firebaseUser: FirebaseUser) {
        showProgressDialog()
        sendEmail(firebaseUser)
    }

    private fun isEmailVerified(firebaseUser: FirebaseUser) {
        if (firebaseUser.isEmailVerified) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
        hideProgressDialog()
    }

    private fun sessionExpired() {
        hideProgressDialog()
        Snackbar
            .make(binding.clVerifyEmail, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
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
                finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbResendEmailVerification.visibility = View.VISIBLE
        binding.btnResendEmail.text = null
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbVerifyEmail.visibility = View.INVISIBLE
        binding.pbResendEmailVerification.visibility = View.INVISIBLE
        binding.btnResendEmail.text = getString(R.string.resend_email_verification)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}