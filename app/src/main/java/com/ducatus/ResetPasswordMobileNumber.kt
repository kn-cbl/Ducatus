package com.ducatus

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_reset_password_mobile_number.*
import java.util.regex.Pattern

class ResetPasswordMobileNumber : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password_mobile_number)

        tvResetPasswordEmailLink.setOnClickListener {
            resetEmailLink()
        }

        imgBtnResetPasswordMobileNumberBack.setOnClickListener {
            onBackPressed()
        }

        btnResetPasswordMobileNumber.setOnClickListener {
            resetPassword()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun resetEmailLink() {
        startActivity(Intent(this, ResetPasswordEmail::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun resetPassword() {
        val mobileNumber = etResetPasswordMobileNumber.text.toString().trim {it <= ' '}

        when {
            TextUtils.isEmpty(mobileNumber) -> {
                Toast.makeText(this, "Please enter mobile number", Toast.LENGTH_SHORT).show()
            }

            else -> {
                mobileNumberExists(mobileNumber)
            }
        }
    }

    private fun mobileNumberExists(mobileNumber: String) {
        database = Firebase.database
        databaseReference = database.getReference("users")

        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var mobileNumberExists = false

                for (child in snapshot.children) {
                    if(mobileNumber == child.child("mobile_number").value.toString()) {
                        mobileNumberExists = true
                        break
                    }
                }

                if (mobileNumberExists) {
                    if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        sendSMS(mobileNumber)
                        Toast.makeText(applicationContext, "Sent", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        ActivityCompat.requestPermissions(this@ResetPasswordMobileNumber, arrayOf(android.Manifest.permission.SEND_SMS), 100)
                    }
                }
                else {
                    Toast.makeText(applicationContext, "User does not exist", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
            }
        })
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