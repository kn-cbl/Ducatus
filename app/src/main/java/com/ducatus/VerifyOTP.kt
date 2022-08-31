package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_verify_otp.*

class VerifyOTP : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var crypto: Crypto
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        imgBtnVerifyOTPBack.setOnClickListener {
            onBackPressed()
        }

        btnVerifyOTP.setOnClickListener {
//            verifyCode()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

//    private fun verifyCode() {
//        val code = etOTP.text.toString().trim {it <= ' '}
//
//        when {
//            TextUtils.isEmpty(code) -> {
//                Toast.makeText(this, "Please enter code", Toast.LENGTH_SHORT).show()
//            }
//
//            else -> {
//                val otp = intent.getStringExtra("code")
//                if(code == otp) {
//                    val method = intent.getIntExtra("method", 0)
//                    readData(method)
//                }
//                else {
//                    Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    private fun readData(method: Int) {
        database = Firebase.database
        databaseReference = database.getReference("users")

        if(method == 1) {
            databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val email: String = intent.getStringExtra("email").toString()
                    var password: String? = null

                    for(child in snapshot.children) {
                        if(email == child.child("email").value.toString()) {
                            password = child.child("password").value.toString()
                            break
                        }
                    }

                    if (password != null) {
//                        login(email, password)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext, "Unknown error occurred, please try again", Toast.LENGTH_LONG).show()
                }
            })
        }
        else if (method == 2) {
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
//                        login(email, password!!)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
                }
            })
        }
    }

//    private fun login(email: String, password: String) {
//        crypto = Crypto()
//        auth = Firebase.auth
//        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, crypto.decrypt(password).toString())
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    startActivity(Intent(this, ResetPassword::class.java))
//                    finish()
//                }
//                else {
//                    Toast.makeText(this, "Unknown error, please try again", Toast.LENGTH_LONG).show()
//                }
//            }
//    }
}