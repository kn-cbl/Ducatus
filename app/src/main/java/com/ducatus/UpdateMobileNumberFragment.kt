package com.ducatus

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUpdateMobileNumberBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class UpdateMobileNumberFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUpdateMobileNumberBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var mobileNumber: String
    private lateinit var options: PhoneAuthOptions
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var storedVerificationId: String
    private var mobileNumberRegex = "^[89][0-9]{9}$"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentUpdateMobileNumberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputObserver()

        binding.btnUpdateMobileNumber.setOnClickListener {
            // validate mobile number -> check if mobile number exists -> send verification code
            validateMobileNumber()
        }

        binding.btnVerifyMobileNumber.setOnClickListener {
            // verify code -> store data -> update firebase
            verifyCode()
        }

        binding.tvResendMobileNumberVerification.setOnClickListener {
            resendVerificationCode(mobileNumber, resendToken)
        }
    }

    private fun inputObserver() {
        binding.tfUpdateMobileNumber.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_empty)
            else if (!mobileNumberRegex.toRegex().matches(text!!)) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_invalid)
            else binding.tfUpdateMobileNumber.error = null
        }
        binding.tfVerifyMobileNumber.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_empty)
            else binding.tfVerifyMobileNumber.error = null
        }
    }

    private fun validateMobileNumber() {
        clearErrors()

        mobileNumber = binding.tfUpdateMobileNumber.editText?.text.toString().trim { it <= ' ' }
        if (mobileNumberRegex.toRegex().matches(mobileNumber)) {
            mobileNumberExists(mobileNumber)
        }
        else {
            if (!mobileNumberRegex.toRegex().matches(mobileNumber)) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_invalid)
            if (TextUtils.isEmpty(mobileNumber)) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_empty)
        }
    }

    private fun mobileNumberExists(mobileNumber: String) {
        showProgressDialog()
        rootLayout = activity.findViewById(R.id.clUserProfile)

        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var numberKey = false

                for (child in snapshot.children) {
                    if(mobileNumber == child.child("mobile_number").value.toString()) {
                        numberKey = true
                        break
                    }
                }

                if (!numberKey) {
                    sendVerificationCode(mobileNumber)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, getString(R.string.mobile_number_exists), Snackbar.LENGTH_LONG)
                        .show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, error.message, Snackbar.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun verifyCode() {
        clearErrors()
        val verificationCode = binding.tfVerifyMobileNumber.editText?.text.toString().trim {it <= ' '}
        when {
            TextUtils.isEmpty(verificationCode) -> binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_empty)
            verificationCode.length < 6 -> binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_error)

            else -> {
                val firebaseUser: FirebaseUser? = auth.currentUser
                if (firebaseUser != null) {
                    showProgressDialog()
                    updateDB(firebaseUser.uid, mobileNumber, verificationCode)
                }
                else {
                    sessionExpired()
                }
            }
        }
    }

    private fun updateDB(uid: String, mobileNumber: String, verificationCode: String) {
        databaseReference = database.getReference("users/" + uid + "/mobile_number")
        databaseReference.setValue(mobileNumber)
            .addOnSuccessListener {
                val phoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, verificationCode)
                updateMobileNumber(phoneAuthCredential)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Failed to update mobile number", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { updateDB(uid, mobileNumber, verificationCode) }
                    .show()
            }
    }

    private fun updateMobileNumber(phoneAuthCredential: PhoneAuthCredential) {
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.updatePhoneNumber(phoneAuthCredential)
                .addOnSuccessListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "Successfully updated mobile number", Snackbar.LENGTH_LONG)
                        .show()

                    val action = UpdateMobileNumberFragmentDirections.actionUpdateMobileNumberFragmentToUserProfileFragment()
                    findNavController().navigate(action)
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, it.localizedMessage!!, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") {updateMobileNumber(phoneAuthCredential) }
                        .show()
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun sendVerificationCode(mobileNumber: String) {
        showProgressDialog()
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                hideProgressDialog()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, e.localizedMessage!!, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") { sendVerificationCode(mobileNumber) }
                        .show()
                }
                else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, e.localizedMessage!!, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") { sendVerificationCode(mobileNumber) }
                        .show()
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                binding.llUpdateMobileNumber.visibility = View.GONE
                binding.llVerifyMobileNumber.visibility = View.VISIBLE
                binding.tvUpdateMobileNumber.text = "0$mobileNumber"
                storedVerificationId = verificationId
                resendToken = token

                hideProgressDialog()
                startTimer()
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

    private fun resendVerificationCode(mobileNumber: String, resendToken: PhoneAuthProvider.ForceResendingToken) {
        showProgressDialog()
        options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+63$mobileNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun startTimer() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvResendMobileNumberVerification.setTextColor(ContextCompat.getColor(activity,R.color.gray_text))
                binding.tvResendMobileNumberVerification.text = "Resend in " + millisUntilFinished / 1000
                binding.tvResendMobileNumberVerification.isEnabled = false
            }
            override fun onFinish() {
                binding.tvResendMobileNumberVerification.setTextColor(ContextCompat.getColor(activity,R.color.green_primary))
                binding.tvResendMobileNumberVerification.text = getString(R.string.resend_verification_code)
                binding.tvResendMobileNumberVerification.isEnabled = true
            }
        }.start()
    }

    private fun sessionExpired() {
        hideProgressDialog()
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        activity.finish()
    }

    private fun showProgressDialog() {
        binding.btnVerifyMobileNumber.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.light_gray_text)
        binding.pbUpdateMobileNumber.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnVerifyMobileNumber.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        binding.pbUpdateMobileNumber.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun clearErrors() {
        binding.tfUpdateMobileNumber.error = null
        binding.tfVerifyMobileNumber.error = null
    }
}