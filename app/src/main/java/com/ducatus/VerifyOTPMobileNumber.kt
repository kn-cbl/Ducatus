package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_verify_otp_mobile_number.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class VerifyOTPMobileNumber : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var generatedOTP: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp_mobile_number)

        tvVerifyOTPUserMobile.text = intent.getStringExtra("mobileNumber").toString()
        generatedOTP = intent.getStringExtra("code").toString()

        inputObserver()

        imgBtnVerifyOTPMobileBack.setOnClickListener {
            onBackPressed()
        }

        btnVerifyOTPMobile.setOnClickListener {
            verifyCode(generatedOTP)
        }

        tvResendOTPMobile.setOnClickListener {
            resendOTP()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun inputObserver() {
        val editText: Array<EditText> = arrayOf(etOTPMobile1, etOTPMobile2, etOTPMobile3, etOTPMobile4)

        etOTPMobile1.doOnTextChanged { text, start, count, after ->
            if (text!!.length == 1) editText[1].requestFocus()
        }

        etOTPMobile2.doOnTextChanged { text, start, count, after ->
            if (text!!.length == 1) editText[2].requestFocus()
            else if (text.isEmpty()) editText[0].requestFocus()
        }

        etOTPMobile3.doOnTextChanged { text, start, count, after ->
            if (text!!.length == 1) editText[3].requestFocus()
            else if (text.isEmpty()) editText[1].requestFocus()
        }

        etOTPMobile4.doOnTextChanged { text, start, count, after ->
            if (text!!.isEmpty()) editText[2].requestFocus()
        }
    }

    private fun verifyCode(generatedOTP: String) {
        tvVerifyOTPMobileError.text = ""
        pbVerifyOTPMobile.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        val otp1 = etOTPMobile1.text.toString().trim {it <= ' '}
        val otp2 = etOTPMobile2.text.toString().trim {it <= ' '}
        val otp3 = etOTPMobile3.text.toString().trim {it <= ' '}
        val otp4 = etOTPMobile4.text.toString().trim {it <= ' '}

        when {
            otp1.isEmpty() || otp2.isEmpty() || otp3.isEmpty() || otp4.isEmpty() -> {
                pbVerifyOTPMobile.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                tvVerifyOTPMobileError.text = "Invalid code, please try again"
            }

            else -> {
                val code = otp1 + otp2 + otp3 + otp4
                if(code == generatedOTP) {
                    readData()
                }
                else {
                    pbVerifyOTPMobile.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    tvVerifyOTPMobileError.text = "Invalid code, please try again"
                }
            }
        }
    }

    private fun readData() {
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mobileNumber: String = intent.getStringExtra("mobileNumber").toString()
                var email: String? = null
                var password: String? = null

                for(child in snapshot.children) {
                    if(mobileNumber == child.child("mobile_number").value.toString()) {
                        email = child.child("email").value.toString()
                        password = child.child("password").value.toString()
                        break
                    }
                }

                if (email != null && password != null) {
                        login(email, password)
                }
                else {
                    pbVerifyOTPMobile.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    tvVerifyOTPMobileError.text = "Unknown error occurred, please try again"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                pbVerifyOTPMobile.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                Log.e("databaseReference", error.message)
                tvVerifyOTPMobileError.text = "Unknown error occurred, please try again"
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
                    pbVerifyOTPMobile.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                    Log.e("authError", "Auth failed")
                    tvVerifyOTPMobileError.text = "Unknown error occurred, please try again"
                }
            }
    }

    private fun resendOTP() {
        val mobileNumber = intent.getStringExtra("mobileNumber").toString()
        sendSMS(mobileNumber)
    }

    private fun sendSMS(mobileNumber: String) {
        val otp = generateOTP()
        val smsManager: SmsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(mobileNumber, null, "OTP: $otp", null, null)
    }

    private fun generateOTP(): String {
        val randomPin = (Math.random() * 9000).toInt() + 1000
        return randomPin.toString()
    }
}