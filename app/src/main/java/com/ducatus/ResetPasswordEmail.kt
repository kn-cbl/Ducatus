package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.ducatus.databinding.ActivityResetPasswordEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ResetPasswordEmail : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityResetPasswordEmailBinding
    private lateinit var appExecutors: AppExecutors
    private lateinit var mailConfig: MailConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password_email)

        binding = ActivityResetPasswordEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        generateOTP()

        binding.tvResetPasswordMobileLink.setOnClickListener {
            resetMobileLink()
        }

        binding.imgBtnResetPasswordEmailBack.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.btnResetPasswordEmail.setOnClickListener {
            // validate credentials -> send email with generated otp
            validateCredentials()
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

    private fun validateCredentials() {
        binding.tvResetPasswordEmailErrorAuth.text = ""
        binding.tvResetPasswordEmailError.text = ""

        val email = binding.etResetPasswordEmail.text.toString().trim {it <= ' '}
        if (TextUtils.isEmpty(email)) {
            binding.tvResetPasswordEmailError.text = "Please enter email"
        }
        else {
            disableWindow()
            auth = Firebase.auth
            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                .addOnSuccessListener { task ->
                    if(!task.signInMethods?.isEmpty()!!) {
                        appExecutors = AppExecutors()
                        sendEmail(email)
                    }
                    else {
                        enableWindow()
                        binding.tvResetPasswordEmailErrorAuth.text = "User does not exist"
                    }
                }
                .addOnFailureListener {
                    enableWindow()
                    binding.tvResetPasswordEmailErrorAuth.text = "User does not exist"
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
                mm.subject = "Ducatus Verification Code"
                mm.setText("$otp is your verification code.")
                Transport.send(mm)

                appExecutors.mainThread().execute {
                    val intent = Intent(this, VerifyOTPEmail::class.java)
                    intent.putExtra("code", otp)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            catch (e: MessagingException) {
                enableWindow()
                Log.e("sendEmailError", e.toString())
                binding.tvResetPasswordEmailErrorAuth.text = e.message
            }
        }
    }

    private fun generateOTP(): String {
        val randomPin = (Math.random() * 900000).toInt() + 1000
        return randomPin.toString()
    }

    private fun enableWindow() {
        binding.btnResetPasswordEmail.setBackgroundResource(R.drawable.green_button)
        binding.pbResetPasswordEmail.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnResetPasswordEmail.setBackgroundResource(R.drawable.btn_disabled)
        binding.pbResetPasswordEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}