package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
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
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password_email)

        binding = ActivityResetPasswordEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        generateOTP()
        inputObserver()

        binding.tvResetPasswordMobileLink.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, ResetPasswordMobileNumber::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.imgBtnResetPasswordEmailBack.setOnClickListener {
            clearErrors()
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

    private fun inputObserver() {
        binding.tfResetPasswordEmail.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfResetPasswordEmail.error = getString(R.string.email_empty)
            else if (!emailRegex.toRegex().matches(text!!)) binding.tfResetPasswordEmail.error = getString(R.string.email_invalid)
            else binding.tfResetPasswordEmail.error = null
        }
    }

    private fun validateCredentials() {
        clearErrors()

        val email = binding.tfResetPasswordEmail.editText?.text.toString().trim {it <= ' '}
        if (emailRegex.toRegex().matches(email)) {
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
                        binding.tvResetPasswordEmailErrorAuth.setText(R.string.user_does_not_exist)
                    }
                }
                .addOnFailureListener {
                    enableWindow()
                    binding.tvResetPasswordEmailErrorAuth.setText(R.string.user_does_not_exist)
                }
        }
        else {
            if (!emailRegex.toRegex().matches(email)) binding.tfResetPasswordEmail.error = getString(R.string.email_invalid)
            if (TextUtils.isEmpty(email)) binding.tfResetPasswordEmail.error = getString(R.string.email_empty)
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
        val otp = (Math.random() * 900000).toInt() + 1000
        return otp.toString()
    }

    private fun enableWindow() {
        binding.btnResetPasswordEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbResetPasswordEmail.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnResetPasswordEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbResetPasswordEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvResetPasswordEmailErrorAuth.text = ""
        binding.tfResetPasswordEmail.error = null
    }
}