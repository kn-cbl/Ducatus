package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
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

class UpdateMobileNumberFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentUpdateMobileNumberBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var mobileNumber: String
    private lateinit var options: PhoneAuthOptions
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var storedVerificationId: String
    private var mobileNumberRegex = "^[89][0-9]{9}$"
    private var updated: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llUserProfile)
        binding = FragmentUpdateMobileNumberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()

        binding.btnUpdateMobileNumberCancel.setOnClickListener {
            dismiss()
        }

        binding.btnUpdateMobileNumberConfirm.setOnClickListener {
            // validate mobile number -> check if mobile number exists -> send verification code
            validateMobileNumber()
        }

        binding.btnUpdateMobileVerify.setOnClickListener {
            // verify code -> store data -> update firebase
            verifyCode()
        }

        binding.btnUpdateMobileResendVerify.setOnClickListener {
            resendVerificationCode(mobileNumber, resendToken)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (updated) {
            val fragment = parentFragmentManager.findFragmentById(R.id.fcUserProfile)
            if (fragment is DialogInterface.OnDismissListener) {
                (fragment as DialogInterface.OnDismissListener?)?.onDismiss(dialog)
            }
        }
    }

    private fun inputObserver() {
        binding.tfUpdateMobileNumber.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_empty)
            else if (!mobileNumberRegex.toRegex().matches(text)) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_invalid)
            else binding.tfUpdateMobileNumber.error = null
        }
        binding.tfVerifyMobileNumber.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_empty)
            else binding.tfVerifyMobileNumber.error = null
        }
    }

    private fun validateMobileNumber() {
        clearErrors()

        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

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
        database = Firebase.database
        databaseReference = database.getReference("users")
        databaseReference.get()
            .addOnSuccessListener {
                var numberKey = false

                for (child in it.children) {
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
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!.toString(), Snackbar.LENGTH_LONG)
                    .show()
            }
    }

    private fun verifyCode() {
        clearErrors()

        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val verificationCode = binding.tfVerifyMobileNumber.editText?.text.toString().trim {it <= ' '}
        when {
            TextUtils.isEmpty(verificationCode) -> binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_empty)
            verificationCode.length < 6 -> binding.tfVerifyMobileNumber.error = getString(R.string.verification_code_error)

            else -> {
                val firebaseUser: FirebaseUser? = auth.currentUser
                if (firebaseUser != null) {
                    updateDB(firebaseUser.uid, mobileNumber, verificationCode)
                }
                else {
                    sessionExpired()
                }
            }
        }
    }

    private fun updateDB(uid: String, mobileNumber: String, verificationCode: String) {
        showProgressDialog()
        databaseReference = database.getReference("users").child(uid).child("mobile_number")
        databaseReference.setValue(mobileNumber)
            .addOnSuccessListener {
                val phoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, verificationCode)
                updateMobileNumber(phoneAuthCredential)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to update mobile number, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateDB(uid, mobileNumber, verificationCode) }
                    .show()
            }
    }

    private fun updateMobileNumber(phoneAuthCredential: PhoneAuthCredential) {
        showProgressDialog()
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.updatePhoneNumber(phoneAuthCredential)
                .addOnSuccessListener {
                    hideProgressDialog()
                    updated = true
                    dismiss()
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "Unable to update mobile number, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { updateMobileNumber(phoneAuthCredential) }
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
                        .setAction(getString(R.string.retry)) { sendVerificationCode(mobileNumber) }
                        .show()
                }
                else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, e.localizedMessage!!, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { sendVerificationCode(mobileNumber) }
                        .show()
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                val phoneNumber = "0$mobileNumber"

                disableResendButton()
                binding.llUpdateMobileNumber.visibility = View.GONE
                binding.llVerifyMobileNumber.visibility = View.VISIBLE
                binding.tvUpdateMobileNumber.text = phoneNumber

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
                        .setAction(getString(R.string.retry)) { resendVerificationCode(mobileNumber, resendToken) }
                        .show()
                }
                else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, e.localizedMessage!!, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { resendVerificationCode(mobileNumber, resendToken) }
                        .show()
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                hideProgressDialog()
                startTimer()
            }
        }

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
                val message = "Resend in " + millisUntilFinished / 1000
                binding.btnUpdateMobileResendVerify.text = message
            }
            override fun onFinish() {
                enableResendButton()
            }
        }.start()
    }

    private fun sessionExpired() {
        hideProgressDialog()
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbUpdateMobileNumber.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbUpdateMobileNumber.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun enableResendButton() {
        binding.btnUpdateMobileResendVerify.setTextColor(ContextCompat.getColor(activity,R.color.green_primary))
        binding.btnUpdateMobileResendVerify.text = getString(R.string.resend_verification_code)
        binding.btnUpdateMobileResendVerify.isEnabled = true
    }

    private fun disableResendButton() {
        binding.btnUpdateMobileResendVerify.setTextColor(ContextCompat.getColor(activity,R.color.darker_gray))
        binding.btnUpdateMobileResendVerify.isEnabled = false
    }

    private fun clearErrors() {
        binding.tfUpdateMobileNumber.error = null
        binding.tfVerifyMobileNumber.error = null
    }
}