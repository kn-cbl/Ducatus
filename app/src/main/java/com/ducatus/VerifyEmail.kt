package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_verify_email.*

class VerifyEmail : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            isEmailVerified(firebaseUser)
            sendEmail(firebaseUser)
        }

        btnResendEmail.setOnClickListener {
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
            }
        }
    }

    private fun reloadUser() {
        var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        firebaseUser?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                firebaseUser = FirebaseAuth.getInstance().currentUser
                isEmailVerified(firebaseUser!!)
            }
            else {
                pbVerifyEmail.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                Log.e("reloadUser", task.exception!!.message.toString())
                Toast.makeText(this, task.exception!!.message.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resendEmail(firebaseUser: FirebaseUser) {
        pbVerifyEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        sendEmail(firebaseUser)
    }

    private fun isEmailVerified(firebaseUser: FirebaseUser) {
        if (firebaseUser.isEmailVerified) {
            pbVerifyEmail.visibility = View.INVISIBLE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

            val intent = Intent(this, Homescreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("loginMethod", 1)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }
}