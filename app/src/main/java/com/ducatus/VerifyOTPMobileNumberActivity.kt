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
    private var status: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp_mobile_number)

        binding = ActivityVerifyOtpMobileNumberBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val mobileNumber = intent.getStringExtra("mobileNumber").toString()
        binding.tvVerifyOTPUserMobile.text = "0$mobileNumber"
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
        binding.tvVerifyOTPMobileError.text = ""
        val code = binding.etOTPMobile.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(code)) {
            binding.tvVerifyOTPMobileError.setText(R.string.verification_code_empty)
        }
        else {
            disableWindow()
            val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, code)

            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
                else {
                    enableWindow()
                    binding.tvVerifyOTPMobileError.setText(R.string.verification_code_error)
                }
            }
        }
    }

    private fun sendVerificationCode(mobileNumber: String) {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // Refer to Firebase documentation
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("complete", "onVerificationCompleted:$credential")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext, R.color.green_primary))
                binding.tvResendOTPMobile.setText(R.string.resend_verification_code)
                binding.tvResendOTPMobile.isEnabled = true
                status = false

                binding.pbVerifyOTPMobile.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                Log.w("failed", "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Log.e("error", "Invalid request")
                }
                else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    binding.tvVerifyOTPMobileError.setText(R.string.mobile_auth_request_error)
                    Log.e("error", "Too many requests, please try again")
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                enableWindow()

                Log.d("verifyId", "onCodeSent:$verificationId")
                Log.d("token", "onCodeSent:$token")

                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId
                resendToken = token
                status = true

                startTimer()
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
        binding.tvVerifyOTPMobileError.text = ""

        if(status) {
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
    }

    private fun startTimer() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext,R.color.gray_text))
                binding.tvResendOTPMobile.text = "Resend in " + millisUntilFinished / 1000
                binding.tvResendOTPMobile.isEnabled = false
            }
            override fun onFinish() {
                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext,R.color.green_primary))
                binding.tvResendOTPMobile.setText(R.string.resend_verification_code)
                binding.tvResendOTPMobile.isEnabled = true
            }
        }.start()
    }

    private fun enableWindow() {
        binding.btnVerifyOTPMobile.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.green_primary)
        binding.pbVerifyOTPMobile.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)    }

    private fun disableWindow() {
        binding.btnVerifyOTPMobile.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.light_gray_text)
        binding.pbVerifyOTPMobile.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}