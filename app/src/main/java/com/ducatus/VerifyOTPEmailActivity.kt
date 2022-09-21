package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.ducatus.databinding.ActivityVerifyOtpEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class VerifyOTPEmailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityVerifyOtpEmailBinding
    private lateinit var appExecutors: AppExecutors
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mailConfig: MailConfig
    private var generatedOTP: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp_email)

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
        binding.tvVerifyOTPEmailError.text = ""
        val code = binding.etOTPEmail.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(code)) {
            binding.tvVerifyOTPEmailError.setText(R.string.verification_code_error)
        }
        else {
            disableWindow()
            if(code == generatedOTP) {
                readData()
            }
            else {
                enableWindow()
                binding.tvVerifyOTPEmailError.setText(R.string.verification_code_error)
            }
        }
    }

    private fun readData() {
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val email = intent.getStringExtra("email").toString()
                var password: String? = null

                for(child in snapshot.children) {
                    if (email == child.child("email").value.toString()) {
                        password = child.child("password").value.toString()
                        break
                    }
                }

                if (password != null) {
                    login(email, password)
                }
                else {
                    enableWindow()
                    binding.tvVerifyOTPEmailError.setText(R.string.unknown_error)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                enableWindow()
                Log.e("databaseError", error.message)
                binding.tvVerifyOTPEmailError.setText(R.string.unknown_error)
            }
        })
    }

    private fun login(email: String, password: String) {
        crypto = Crypto()
        auth = Firebase.auth
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, crypto.decrypt(password).toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
                else {
                    enableWindow()
                    Log.e("authError", "Auth failed")
                    binding.tvVerifyOTPEmailError.setText(R.string.unknown_error)
                }
            }
    }

    private fun resendOTP() {
        val email = intent.getStringExtra("email").toString()
        sendEmail(email)
    }

    private fun sendEmail(email: String){
        disableWindow()

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
                generatedOTP = otp

                val mm = MimeMessage(session)
                mm.setFrom(InternetAddress(mailConfig.senderEmail))
                mm.addRecipient(Message.RecipientType.TO, InternetAddress(email))
                mm.subject = "Test Email"
                mm.setText("OTP: $otp")
                Transport.send(mm)

                appExecutors.mainThread().execute {
                    enableWindow()
                    startTimer()
                }
            }
            catch (e: MessagingException) {
                enableWindow()
                Log.e("sendEmailError", e.toString())
                binding.tvVerifyOTPEmailError.text = e.message
            }
        }
    }

    private fun generateOTP(): String {
        val randomPin = (Math.random() * 9000).toInt() + 1000
        return randomPin.toString()
    }

    private fun startTimer() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvResendOTPEmail.setTextColor(ContextCompat.getColor(applicationContext,R.color.gray_text))
                binding.tvResendOTPEmail.text = "Resend in " + millisUntilFinished / 1000
                binding.tvResendOTPEmail.isEnabled = false
            }
            override fun onFinish() {
                binding.tvResendOTPEmail.setTextColor(ContextCompat.getColor(applicationContext,R.color.green_primary))
                binding.tvResendOTPEmail.setText(R.string.resend_verification_code)
                binding.tvResendOTPEmail.isEnabled = true
            }
        }.start()
    }

    private fun enableWindow() {
        binding.btnVerifyOTPEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnVerifyOTPEmail.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbVerifyOTPEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

}