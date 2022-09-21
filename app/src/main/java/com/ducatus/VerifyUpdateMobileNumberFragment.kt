package com.ducatus

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentUserProfileBinding
import com.ducatus.databinding.FragmentVerifyUpdateMobileNumberBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class VerifyUpdateMobileNumberFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentVerifyUpdateMobileNumberBinding
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var options: PhoneAuthOptions
    private lateinit var storedVerificationId: String
    private var mobileNumberRegex = "^[89][0-9]{9}$"
    private val args: VerifyUpdateMobileNumberFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentVerifyUpdateMobileNumberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sendVerificationCode(args.mobileNumber)
        inputObserver()

        binding.btnVerifyMobileNumber.setOnClickListener {
            validateMobileNumber()
        }
    }

    private fun inputObserver() {
        binding.tfVerifyMobileNumber.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfVerifyMobileNumber.error = getString(R.string.mobile_number_empty)
            else if (!mobileNumberRegex.toRegex().matches(text!!)) binding.tfVerifyMobileNumber.error = getString(R.string.mobile_number_invalid)
            else binding.tfVerifyMobileNumber.error = null
        }
    }

    private fun validateMobileNumber() {
        clearErrors()

        val verificationCode = binding.tfVerifyMobileNumber.editText?.text.toString().trim {it <= ' '}
        when {
            TextUtils.isEmpty(verificationCode) -> binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_empty)
            verificationCode.length < 6 -> binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_error)

            else -> {
                val credential = PhoneAuthProvider.getCredential(storedVerificationId, verificationCode)
                updateMobileNumber(credential)
            }
        }
    }

    private fun updateMobileNumber(phoneAuthCredential: PhoneAuthCredential) {
        val authUser = auth.currentUser
        authUser?.updatePhoneNumber(phoneAuthCredential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val action = VerifyUpdateMobileNumberFragmentDirections.actionVerifyUpdateMobileNumberFragmentToUserProfileFragment()
                findNavController().navigate(action)
            }
            else {
                binding.tfVerifyMobileNumber.error = "error"
            }
        }
    }

    private fun sendVerificationCode(mobileNumber: String) {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // Refer to Firebase documentation
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Toast.makeText(activity, "complete", Toast.LENGTH_SHORT).show()
                Log.d("complete", "onVerificationCompleted:$credential")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(activity, "failed", Toast.LENGTH_SHORT).show()

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Log.e("error", "Invalid request")
                }
                else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    binding.tfVerifyMobileNumber.error = getString(R.string.mobile_auth_request_error)
                    Log.e("error", "Too many requests, please try again")
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
//                enableWindow()

                Toast.makeText(activity, "sent", Toast.LENGTH_SHORT).show()
                Log.d("verifyId", "onCodeSent:$verificationId")
                Log.d("token", "onCodeSent:$token")

                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId
//                resendToken = token
//                status = true

//                startTimer()
            }
        }

        auth = Firebase.auth
        options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+63$mobileNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

//    private fun startTimer() {
//        object : CountDownTimer(60000, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext,R.color.gray_text))
//                binding.tvResendOTPMobile.text = "Resend in " + millisUntilFinished / 1000
//                binding.tvResendOTPMobile.isEnabled = false
//            }
//            override fun onFinish() {
//                binding.tvResendOTPMobile.setTextColor(ContextCompat.getColor(applicationContext,R.color.green_primary))
//                binding.tvResendOTPMobile.setText(R.string.resend_verification_code)
//                binding.tvResendOTPMobile.isEnabled = true
//            }
//        }.start()
//    }

    private fun enableWindow() {
        binding.btnVerifyMobileNumber.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
//        binding.pbLogin.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        binding.btnVerifyMobileNumber.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.light_gray_text)
//        binding.pbLogin.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tfVerifyMobileNumber.error = null
    }
}