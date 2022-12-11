package com.ducatus

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivityResetPasswordEmailBinding
import com.ducatus.utils.AppExecutors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ResetPasswordEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetPasswordEmailBinding
    private lateinit var appExecutors: AppExecutors
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        generateOTP()
        inputObserver()

        binding.tvResetPasswordMobileLink.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, ResetPasswordMobileNumberActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.imgBtnResetPasswordEmailBack.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, LoginActivity::class.java))
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
            if (text == null || text.isEmpty()) binding.tfResetPasswordEmail.error = getString(R.string.email_empty)
            else if (!emailRegex.toRegex().matches(text)) binding.tfResetPasswordEmail.error = getString(R.string.email_invalid)
            else binding.tfResetPasswordEmail.error = null
        }
    }

    private fun validateCredentials() {
        clearErrors()

        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val email = binding.tfResetPasswordEmail.editText?.text.toString().trim {it <= ' '}
        if (emailRegex.toRegex().matches(email)) {
            showProgressDialog()
            val auth = Firebase.auth
            auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener {
                    if(!it.signInMethods?.isEmpty()!!) {
                        appExecutors = AppExecutors()
                        sendEmail(email)
                    }
                    else {
                        hideProgressDialog()
                        binding.tvResetPasswordEmailErrorAuth.text = getString(R.string.user_does_not_exist)
                    }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    binding.tvResetPasswordEmailErrorAuth.text = getString(R.string.user_does_not_exist)
                }
        }
        else {
            if (!emailRegex.toRegex().matches(email)) binding.tfResetPasswordEmail.error = getString(R.string.email_invalid)
            if (TextUtils.isEmpty(email)) binding.tfResetPasswordEmail.error = getString(R.string.email_empty)
        }
    }

    private fun sendEmail(email: String){
        appExecutors.diskIO().execute {
            val props = System.getProperties()
            props["mail.smtp.host"] = BuildConfig.SMTP_HOST
            props["mail.smtp.socketFactory.port"] = BuildConfig.SMTP_PORT
            props["mail.smtp.socketFactory.class"] = BuildConfig.SMTP_CLASS
            props["mail.smtp.auth"] = BuildConfig.SMTP_AUTH
            props["mail.smtp.port"] = BuildConfig.SMTP_PORT

            val session = Session.getInstance(props,
                object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(BuildConfig.MAIL_SENDER, BuildConfig.MAIL_KEY)
                    }
                })

            try {
                val otp = generateOTP()
                val mm = MimeMessage(session)
                mm.setFrom(InternetAddress(BuildConfig.MAIL_SENDER))
                mm.addRecipient(Message.RecipientType.TO, InternetAddress(email))
                mm.subject = "Ducatus Verification Code"
                mm.setText("$otp is your verification code.")
                Transport.send(mm)

                appExecutors.mainThread().execute {
                    hideProgressDialog()
                    val intent = Intent(this, VerifyOTPEmailActivity::class.java)
                    intent.putExtra("code", otp)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            catch (e: MessagingException) {
                hideProgressDialog()
                binding.tvResetPasswordEmailErrorAuth.text = e.localizedMessage
            }
        }
    }

    private fun generateOTP(): String {
        val otp = (Math.random() * 900000).toInt() + 1000
        return otp.toString()
    }

    private fun showProgressDialog() {
        binding.pbResetPasswordEmail.visibility = View.VISIBLE
        binding.btnResetPasswordEmail.text = null
        binding.btnResetPasswordEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbResetPasswordEmail.visibility = View.INVISIBLE
        binding.btnResetPasswordEmail.text = getString(R.string.send)
        binding.btnResetPasswordEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvResetPasswordEmailErrorAuth.text = ""
        binding.tfResetPasswordEmail.error = null
    }
}