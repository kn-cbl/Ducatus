package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.ducatus.databinding.ActivityVerifyEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class VerifyEmailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityVerifyEmailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            isEmailVerified(firebaseUser)
            sendEmail(firebaseUser)
        }

        binding.btnResendEmail.setOnClickListener {
            if (firebaseUser != null) {
                resendEmail(firebaseUser)
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
        firebaseUser.sendEmailVerification().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("sendEmailVerification", task.exception!!.message.toString())
                binding.tvVerifyEmailError.text = task.exception!!.message
            }
        }
    }

    // reload email verified status of user
    private fun reloadUser() {
        disableWindow()
        var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        firebaseUser?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                firebaseUser = FirebaseAuth.getInstance().currentUser
                isEmailVerified(firebaseUser!!)
            }
            else {
                enableWindow()
                Log.e("reloadUser", task.exception!!.message.toString())
                binding.tvVerifyEmailError.text = task.exception!!.message
            }
        }
    }

    private fun resendEmail(firebaseUser: FirebaseUser) {
        disableWindow()
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
        else {
            enableWindow()
        }
    }

    private fun enableWindow() {
        binding.pbVerifyEmail.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.pbVerifyEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}