package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import com.ducatus.databinding.ActivityVerifyEmailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        binding.ibVerifyEmailBack.setOnClickListener {
            onBackPressed()
        }

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
        FirebaseAuth.getInstance().signOut()
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
                        hideProgressDialogResend()
                        startTimer()
                    }
                    .addOnFailureListener {
                        hideProgressDialogResend()
                        binding.tvVerifyEmailError.text = it.localizedMessage
                    }
            }
        }
        else {
            hideProgressDialogResend()
            sessionExpired()
        }
    }

    // reload email verified status of user
    private fun reloadUser() {
        showProgressDialogVerify()
        var firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.reload()
                .addOnSuccessListener {
                    hideProgressDialogVerify()
                    firebaseUser = auth.currentUser
                    if (firebaseUser?.isEmailVerified == true) verified()
                }
                .addOnFailureListener {
                    hideProgressDialogVerify()
                    binding.tvVerifyEmailError.text = it.localizedMessage
                }
        }
        else {
            hideProgressDialogVerify()
            sessionExpired()
        }
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
            hideProgressDialogResend()
            sessionExpired()
        }
    }

    private fun startTimer() {
        binding.btnResendEmail.isClickable = false
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val message = "Resend in " + millisUntilFinished / 1000
                binding.btnResendEmail.text = message
            }
            override fun onFinish() {
                hideProgressDialogResend()
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
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(applicationContext, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
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
        binding.btnResendEmail.text = getString(R.string.resend_email_verification)
        binding.btnResendEmail.isClickable = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}