package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.ducatus.databinding.ActivityResetPasswordMobileNumberBinding
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ResetPasswordMobileNumber : AppCompatActivity() {
    private lateinit var binding: ActivityResetPasswordMobileNumberBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password_mobile_number)

        binding = ActivityResetPasswordMobileNumberBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tvResetPasswordEmailLink.setOnClickListener {
            resetEmailLink()
        }

        binding.imgBtnResetPasswordMobileNumberBack.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.btnResetPasswordMobileNumber.setOnClickListener {
            // validate credentials -> check if mobile number exists
            validateCredentials()
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

    private fun validateCredentials() {
        binding.tvResetPasswordMobileErrorAuth.text = ""
        binding.tvResetPasswordMobileNumberError.visibility = View.INVISIBLE
        val mobileNumber = binding.etResetPasswordMobileNumber.text.toString().trim {it <= ' '}

        when {
            TextUtils.isEmpty(mobileNumber) -> {
                binding.tvResetPasswordMobileNumberError.visibility = View.VISIBLE
            }

            else -> {
                binding.pbResetPasswordMobileNumber.visibility = View.VISIBLE
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
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

                binding.pbResetPasswordMobileNumber.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                if (mobileNumberExists) {
                    val intent = Intent(applicationContext, VerifyOTPMobileNumber::class.java)
                    intent.putExtra("mobileNumber", mobileNumber)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                else {
                    binding.tvResetPasswordMobileErrorAuth.text = "User does not exist"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                binding.pbResetPasswordMobileNumber.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                Log.e("databaseError", error.message)
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}