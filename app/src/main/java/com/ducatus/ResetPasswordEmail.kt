package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_reset_password_email.*
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ResetPasswordEmail : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var appExecutors: AppExecutors
    private lateinit var mailConfig: MailConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password_email)

        tvResetPasswordMobileLink.setOnClickListener {
            resetMobileLink()
        }

        imgBtnResetPasswordEmailBack.setOnClickListener {
            onBackPressed()
        }

        btnResetPasswordEmail.setOnClickListener {
            resetPassword()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun resetMobileLink() {
        startActivity(Intent(this, ResetPasswordMobileNumber::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun resetPassword() {
        tvResetPasswordEmailErrorAuth.text = ""
        tvResetPasswordEmailError.visibility = View.INVISIBLE

        val email = etResetPasswordEmail.text.toString().trim {it <= ' '}

        when {
            TextUtils.isEmpty(email) -> {
                pbResetPasswordEmail.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvResetPasswordEmailError.visibility = View.VISIBLE
            }

            else -> {
                pbResetPasswordEmail.visibility = View.VISIBLE
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                auth = Firebase.auth
                FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                    .addOnSuccessListener { task ->
                        if(!task.signInMethods?.isEmpty()!!) {
                            appExecutors = AppExecutors()
                            sendEmail(email)
                        }
                        else {
                            pbResetPasswordEmail.visibility = View.INVISIBLE
                            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                            Log.e("authEmail", "user does not exist")
                            tvResetPasswordEmailErrorAuth.text = "User does not exist"
                        }
                    }
                    .addOnFailureListener {
                        pbResetPasswordEmail.visibility = View.INVISIBLE
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                        Log.e("authEmail", "user does not exist")
                        tvResetPasswordEmailErrorAuth.text = "User does not exist"
                    }
            }
        }
    }

    private fun sendEmail(email: String){
        appExecutors.diskIO().execute {
            mailConfig = MailConfig()
            val props = System.getProperties()
            props["mail.smtp.host"] = mailConfig.smtpHost
            props["mail.smtp.socketFactory.port"] = mailConfig.smtpPort
            props["mail.smtp.socketFactory.class"] = mailConfig.smtpClass
            props["mail.smtp.auth"] = mailConfig.smtpAuth
            props["mail.smtp.port"] = mailConfig.smtpPort

            val session = Session.getInstance(props,
                object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(mailConfig.senderEmail, mailConfig.senderPassword)
                    }
                })

            try {
                val otp = generateOTP()
                val mm = MimeMessage(session)
                mm.setFrom(InternetAddress(mailConfig.senderEmail))
                mm.addRecipient(Message.RecipientType.TO, InternetAddress(email))
                mm.subject = "Test Email"
                mm.setText("OTP: $otp")
                Transport.send(mm)

                appExecutors.mainThread().execute {
                    pbResetPasswordEmail.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                    val intent = Intent(this, VerifyOTPEmail::class.java)
                    intent.putExtra("code", otp)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            catch (e: MessagingException) {
                pbResetPasswordEmail.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                Log.e("sendEmailError", e.toString())
                tvResetPasswordEmailErrorAuth.text = e.message
            }
        }
    }

    private fun generateOTP(): String {
        val randomPin = (Math.random() * 9000).toInt() + 1000
        return randomPin.toString()
    }
}