package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.ActivityVerifyOtpEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class VerifyOTPEmail : AppCompatActivity() {
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
        binding.pbVerifyOTPEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        val code = binding.etOTPEmail.text.toString().trim {it <= ' '}

        when {
            code.isEmpty() -> {
                binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                binding.tvVerifyOTPEmailError.text = "Invalid code, please try again"
            }

            else -> {
                if(code == generatedOTP) {
                    readData()
                }
                else {
                    binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    binding.tvVerifyOTPEmailError.text = "Invalid code, please try again"
                }
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
                    binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    binding.tvVerifyOTPEmailError.text = "Unknown error occurred, please try again"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                Log.e("databaseError", error.message)
                binding.tvVerifyOTPEmailError.text = "Unknown error occurred, please try again"
            }
        })
    }

    private fun login(email: String, password: String) {
        crypto = Crypto()
        auth = Firebase.auth
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, crypto.decrypt(password).toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, ResetPassword::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
                else {
                    binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                    Log.e("authError", "Auth failed")
                    binding.tvVerifyOTPEmailError.text = "Unknown error occurred, please try again"
                }
            }
    }

    private fun resendOTP() {
        val email = intent.getStringExtra("email").toString()
        sendEmail(email)
    }

    private fun sendEmail(email: String){
        binding.pbVerifyOTPEmail.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

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
                    binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    Toast.makeText(this, "Successfully resent verification code", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: MessagingException) {
                binding.pbVerifyOTPEmail.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                Log.e("sendEmailError", e.toString())
                binding.tvVerifyOTPEmailError.text = e.message
            }
        }
    }

    private fun generateOTP(): String {
        val randomPin = (Math.random() * 9000).toInt() + 1000
        return randomPin.toString()
    }
}