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
import com.ducatus.databinding.ActivityVerifyOtpEmailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class VerifyOTPEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyOtpEmailBinding
    private var generatedOTP: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        startTimer()

        binding.tvVerifyOTPUserEmail.text = intent.getStringExtra("email").toString()
        generatedOTP = intent.getStringExtra("code").toString()

        binding.imgBtnVerifyOTPEmailBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnVerifyOTPEmail.setOnClickListener {
            verifyCode(generatedOTP)
        }

        binding.tvResendOTPEmail.setOnClickListener {
            resendOTP()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun verifyCode(generatedOTP: String) {
        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        binding.tvVerifyOTPEmailError.text = ""
        val code = binding.etOTPEmail.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(code)) {
            binding.tvVerifyOTPEmailError.text = getString(R.string.verification_code_error)
        }
        else {
            showProgressDialog()
            if (code == generatedOTP) {
                readData()
            }
            else {
                hideProgressDialog()
                binding.tvVerifyOTPEmailError.text = getString(R.string.verification_code_error)
            }
        }
    }

    private fun readData() {
        val database = Firebase.database
        val databaseReference = database.getReference("users")
        databaseReference.get()
            .addOnSuccessListener {
                val email = intent.getStringExtra("email").toString()
                var password: String? = null

                for (child in it.children) {
                    if (email == child.child("email").value.toString()) {
                        password = child.child("password").value.toString()
                        break
                    }
                }

                if (password != null) {
                    login(email, password)
                }
                else {
                    hideProgressDialog()
                    binding.tvVerifyOTPEmailError.text = getString(R.string.unknown_error)
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvVerifyOTPEmailError.text = getString(R.string.unknown_error)
            }
    }

    private fun login(email: String, password: String) {
        val crypto = Crypto()
        val auth = Firebase.auth
        auth.signInWithEmailAndPassword(email, crypto.decrypt(password).toString())
            .addOnSuccessListener {
                val intent = Intent(this, ResetPasswordActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tvVerifyOTPEmailError.text = getString(R.string.unknown_error)
            }
    }

    private fun resendOTP() {
        val email = intent.getStringExtra("email").toString()
        sendEmail(email)
    }

    private fun sendEmail(email: String){
        showProgressDialog2()
        val appExecutors = AppExecutors()
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
                generatedOTP = otp

                val mm = MimeMessage(session)
                mm.setFrom(InternetAddress(BuildConfig.MAIL_SENDER))
                mm.addRecipient(Message.RecipientType.TO, InternetAddress(email))
                mm.subject = "Ducatus Verification Code"
                mm.setText("$otp is your verification code.")
                Transport.send(mm)

                appExecutors.mainThread().execute {
                    binding.tvResendOTPEmail.setTextColor(ContextCompat.getColor(applicationContext,R.color.darker_gray))
                    binding.tvResendOTPEmail.isEnabled = false

                    hideProgressDialog()
                    startTimer()
                }
            }
            catch (e: MessagingException) {
                hideProgressDialog()
                binding.tvVerifyOTPEmailError.text = e.message
            }
        }
    }

    private fun generateOTP(): String {
        val randomPin = (Math.random() * 900000).toInt() + 1000
        return randomPin.toString()
    }

    private fun startTimer() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val message = "Resend in " + millisUntilFinished / 1000
                binding.tvResendOTPEmail.text = message
            }
            override fun onFinish() {
                binding.tvResendOTPEmail.setTextColor(ContextCompat.getColor(applicationContext,R.color.green_primary))
                binding.tvResendOTPEmail.text = getString(R.string.resend_verification_code)
                binding.tvResendOTPEmail.isEnabled = true
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbVerifyOTPEmail.visibility = View.VISIBLE
        binding.btnVerifyOTPEmail.text = null
        binding.btnVerifyOTPEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun showProgressDialog2() {
        binding.pbResendOTPEmail.visibility = View.VISIBLE
        binding.btnVerifyOTPEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbResendOTPEmail.visibility = View.INVISIBLE
        binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
        binding.btnVerifyOTPEmail.text = getString(R.string.verify)
        binding.btnVerifyOTPEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}