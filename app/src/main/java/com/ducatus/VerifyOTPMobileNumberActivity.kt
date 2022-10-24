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
import com.ducatus.databinding.ActivityVerifyOtpMobileNumberBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class VerifyOTPMobileNumberActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityVerifyOtpMobileNumberBinding
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var options: PhoneAuthOptions
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var storedVerificationId: String
    private lateinit var timer: CountDownTimer
    private var status: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpMobileNumberBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        var mobileNumber = intent.getStringExtra("mobileNumber").toString()
        mobileNumber = "0$mobileNumber"
        binding.tvVerifyOTPUserMobile.text = mobileNumber
        sendVerificationCode(mobileNumber)

        binding.imgBtnVerifyOTPMobileBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnVerifyOTPMobile.setOnClickListener {
            // verify code -> sign in user and send to reset password activity
            verifyCode()
        }

        binding.tvResendOTPMobile.setOnClickListener {
            resendVerificationCode(mobileNumber, resendToken)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun verifyCode() {
        // hide keyboard
        try {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        binding.tvVerifyOTPMobileError.text = ""
        val code = binding.etOTPMobile.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(code)) {
            binding.tvVerifyOTPMobileError.text = getString(R.string.verification_code_empty)
        }
        else {
            showProgressDialog()
            val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, code)
            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    timer.cancel()

                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    binding.tvVerifyOTPMobileError.text = getString(R.string.verification_code_error)
                }
        }
    }

    private fun sendVerificationCode(mobileNumber: String) {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // Refer to Firebase documentation
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext, R.color.green_primary))
                binding.tvResendOTPMobile.text = getString(R.string.resend_verification_code)
                binding.tvResendOTPMobile.isEnabled = true
                status = false

                binding.pbVerifyOTPMobile.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    binding.tvVerifyOTPMobileError.text = e.localizedMessage
                }
                else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    binding.tvVerifyOTPMobileError.text = getString(R.string.mobile_auth_request_error)
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext,R.color.darker_gray))
                binding.tvResendOTPMobile.isEnabled = false

                storedVerificationId = verificationId
                resendToken = token
                status = true

                hideProgressDialog()
                setTimer()
                timer.start()
            }
        }

        auth = Firebase.auth
        options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+63$mobileNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(mobileNumber: String, resendToken: PhoneAuthProvider.ForceResendingToken) {
        showProgressDialog2()
        binding.tvVerifyOTPMobileError.text = ""
        if (status) {
            options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+63$mobileNumber")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .setForceResendingToken(resendToken)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
        else {
            sendVerificationCode(mobileNumber)
        }
        hideProgressDialog()
    }

    private fun setTimer() {
        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val message = "Resend in " + millisUntilFinished / 1000 + "s"
                binding.tvResendOTPMobile.text = message
            }
            override fun onFinish() {
                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext,R.color.green_primary))
                binding.tvResendOTPMobile.text = getString(R.string.resend_verification_code)
                binding.tvResendOTPMobile.isEnabled = true
            }
        }
    }

    private fun showProgressDialog() {
        binding.pbVerifyOTPMobile.visibility = View.VISIBLE
        binding.btnVerifyOTPMobile.text = null
        binding.btnVerifyOTPMobile.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun showProgressDialog2() {
        binding.pbResendOTPMobile.visibility = View.VISIBLE
        binding.btnVerifyOTPMobile.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.gray)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbResendOTPMobile.visibility = View.INVISIBLE
        binding.pbVerifyOTPMobile.visibility = View.INVISIBLE
        binding.btnVerifyOTPMobile.text = getString(R.string.verify)
        binding.btnVerifyOTPMobile.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}