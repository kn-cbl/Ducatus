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
import com.ducatus.databinding.ActivityResetPasswordMobileNumberBinding
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ResetPasswordMobileNumberActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetPasswordMobileNumberBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var mobileNumberRegex = "^[89][0-9]{9}$"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordMobileNumberBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        inputObserver()

        binding.tvResetPasswordEmailLink.setOnClickListener {
            clearErrors()
            startActivity(Intent(this, ResetPasswordEmailActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.imgBtnResetPasswordMobileNumberBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.btnResetPasswordMobileNumber.setOnClickListener {
            // validate credentials -> check if mobile number exists -> send verification code activity
            validateCredentials()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun inputObserver() {
        binding.tfResetPasswordMobile.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfResetPasswordMobile.error = getString(R.string.mobile_number_empty)
            else if (!mobileNumberRegex.toRegex().matches(text!!)) binding.tfResetPasswordMobile.error = getString(R.string.mobile_number_invalid)
            else binding.tfResetPasswordMobile.error = null
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

        val mobileNumber = binding.tfResetPasswordMobile.editText?.text.toString().trim {it <= ' '}
        if (mobileNumberRegex.toRegex().matches(mobileNumber)) {
            showProgressDialog()
            mobileNumberExists(mobileNumber)
        }
        else {
            if (!mobileNumberRegex.toRegex().matches(mobileNumber)) binding.tfResetPasswordMobile.error = getString(R.string.mobile_number_invalid)
            if (TextUtils.isEmpty(mobileNumber)) binding.tfResetPasswordMobile.error = getString(R.string.mobile_number_empty)
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

                hideProgressDialog()
                if (mobileNumberExists) {
                    val intent = Intent(applicationContext, VerifyOTPMobileNumberActivity::class.java)
                    intent.putExtra("mobileNumber", mobileNumber)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                else {
                    binding.tvResetPasswordMobileErrorAuth.text = getString(R.string.user_does_not_exist)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                binding.tvResetPasswordMobileErrorAuth.text = error.message
            }
        })
    }

    private fun showProgressDialog() {
        binding.btnResetPasswordMobileNumber.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbResetPasswordMobileNumber.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnResetPasswordMobileNumber.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbResetPasswordMobileNumber.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tvResetPasswordMobileErrorAuth.text = ""
        binding.tfResetPasswordMobile.error = null
    }
}