package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
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

        sendEmail()

        binding.btnResendEmail.setOnClickListener {
            resendEmail()
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

    private fun sendEmail() {
        showProgressDialogResend()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            if (firebaseUser.isEmailVerified) {
                verified()
            }
            else {
                firebaseUser.sendEmailVerification()
                    .addOnSuccessListener {
                        startTimer()
                    }
                    .addOnFailureListener {
                        binding.tvVerifyEmailError.text = it.localizedMessage
                    }
            }
        }
        else {
            sessionExpired()
        }
        hideProgressDialogResend()
    }

    // reload email verified status of user
    private fun reloadUser() {
        showProgressDialogVerify()
        var firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.reload()
                .addOnSuccessListener {
                    firebaseUser = auth.currentUser
                    if (firebaseUser?.isEmailVerified == true) verified()
                }
                .addOnFailureListener {
                    binding.tvVerifyEmailError.text = it.localizedMessage
                }
        }
        else {
            sessionExpired()
        }
        hideProgressDialogVerify()
    }

    private fun resendEmail() {
        showProgressDialogResend()
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification()
                .addOnSuccessListener {
                    hideProgressDialogResend()
                    startTimer()
                }
                .addOnFailureListener {
                    hideProgressDialogResend()
                    binding.tvVerifyEmailError.text = it.localizedMessage
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun startTimer() {
        binding.btnResendEmail.isEnabled = false
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val message = "Resend in " + millisUntilFinished / 1000
                binding.btnResendEmail.text = message
            }
            override fun onFinish() {
                binding.btnResendEmail.text = getString(R.string.resend_email_verification)
                binding.btnResendEmail.isEnabled = true
            }
        }.start()
    }

    private fun verified() {
        hideProgressDialogVerify()
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }

    private fun sessionExpired() {
        hideProgressDialogVerify()
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

    private fun showProgressDialogVerify() {
        binding.pbVerifyEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialogVerify() {
        binding.pbVerifyEmail.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun showProgressDialogResend() {
        binding.pbResendEmailVerification.visibility = View.VISIBLE
        binding.btnResendEmail.text = null
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialogResend() {
        binding.pbResendEmailVerification.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}