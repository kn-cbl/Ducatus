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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.ducatus.databinding.FragmentUpdateEmailDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UpdateEmailDialogFragment : DialogFragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var binding: FragmentUpdateEmailDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var email: String
    private lateinit var rootLayout: LinearLayout
    private lateinit var timer: CountDownTimer
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"
    private var status: Boolean = false
    private var updated: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentUpdateEmailDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootLayout = activity.findViewById(R.id.llUserProfile)
        inputObserver()

        binding.btnUpdateEmailCancel.setOnClickListener {
            dismiss()
        }

        binding.btnUpdateEmailConfirm.setOnClickListener {
            // validate credentials -> show confirmation -> reauthenticate -> update email -> send email verification
            validateCredentials()
        }

        binding.tvUpdateEmailResendEmail.setOnClickListener {
            resendEmail()
        }
    }

    override fun onResume() {
        super.onResume()
        if (status) reloadUser()
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
        binding.tfUpdateEmail.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfUpdateEmail.error = getString(R.string.new_email_empty)
            else if (!emailRegex.toRegex().matches(text)) binding.tfUpdateEmail.error = getString(R.string.email_invalid)
            else binding.tfUpdateEmail.error = null
        }
        binding.tfUpdateEmailReauthenticate.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfUpdateEmailReauthenticate.error = getString(R.string.password_empty)
            else binding.tfUpdateEmailReauthenticate.error = null
        }
    }

    private fun validateCredentials() {
        // hide keyboard
        try {
            val windowToken: View = dialog!!.window!!.decorView.rootView
            val imm: InputMethodManager = dialog!!.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken.windowToken, 0)
        }
        catch (e: Exception){}

        binding.tfUpdateEmail.error = null
        binding.tfUpdateEmailReauthenticate.error = null

        email = binding.tfUpdateEmail.editText?.text.toString().trim {it <= ' '}
        val password = binding.tfUpdateEmailReauthenticate.editText?.text.toString().trim {it <= ' '}

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            if (firebaseUser.email != email && emailRegex.toRegex().matches(email) && !TextUtils.isEmpty(password)) {
                confirmUpdate(firebaseUser, email, password)
            }
            else {
                if (!emailRegex.toRegex().matches(email)) binding.tfUpdateEmail.error = getString(R.string.email_invalid)
                if (firebaseUser.email == email) binding.tfUpdateEmail.error = getString(R.string.new_email_same)
                if (TextUtils.isEmpty(email)) binding.tfUpdateEmail.error = getString(R.string.new_email_empty)
                if (TextUtils.isEmpty(password)) binding.tfUpdateEmailReauthenticate.error = getString(R.string.password_empty)
            }
        }
        else {
            sessionExpired()
        }
    }

    private fun confirmUpdate(firebaseUser: FirebaseUser, email: String, password: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.update_email_mark))
            .setPositiveButton(resources.getString(R.string.update)) { _, _ -> reauthenticateUser(firebaseUser, email, password) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun reauthenticateUser(firebaseUser: FirebaseUser, email: String, password: String) {
        showProgressDialog()
        val credential = EmailAuthProvider.getCredential(firebaseUser.email.toString(), password)
        firebaseUser.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    unlinkGoogle(firebaseUser, email)
                }
                else {
                    hideProgressDialog()
                    val exception = task.exception as FirebaseAuthException
                    when (exception.errorCode) {
                        "ERROR_WRONG_PASSWORD" -> binding.tfUpdateEmailReauthenticate.error = getString(R.string.password_invalid)
                        else -> binding.tfUpdateEmailReauthenticate.error = exception.localizedMessage
                    }
                }
            }
    }

    private fun unlinkGoogle(firebaseUser: FirebaseUser, email: String) {
        var googleKey = false
        for (provider in firebaseUser.providerData) {
            if (provider.providerId == "google.com") {
                googleKey = true
            }
        }

        if (googleKey) {
            firebaseUser.unlink("google.com")
                .addOnSuccessListener {
                    updateEmail(firebaseUser, email)
                }
                .addOnFailureListener {
                    binding.tfUpdateEmail.error = it.localizedMessage
                }
        }
        else {
            updateEmail(firebaseUser, email)
        }
    }

    private fun updateEmail(firebaseUser: FirebaseUser, email: String) {
        firebaseUser.updateEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateDB(firebaseUser, email)
                }
                else {
                    hideProgressDialog()
                    val exception = task.exception as FirebaseAuthException
                    when (exception.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> binding.tfUpdateEmail.error = getString(R.string.email_exists)
                        else -> binding.tfUpdateEmail.error = exception.localizedMessage
                    }
                }
            }
    }

    private fun updateDB(firebaseUser: FirebaseUser, email: String) {
        database = Firebase.database
        databaseReference = database.getReference("users").child(firebaseUser.uid).child("email")
        databaseReference.setValue(email)
            .addOnSuccessListener {
                sendEmail(firebaseUser)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun sendEmail(firebaseUser: FirebaseUser) {
        showProgressDialog()
        setTimer()
        status = true
        updated = true

        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, "Email updated, email verification sent", Toast.LENGTH_LONG)
                    .show()

                disableResendbutton()
                timer.start()
                binding.tvUpdateEmailResendEmail.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()

                disableResendbutton()
                timer.start()
                binding.tvUpdateEmailResendEmail.visibility = View.VISIBLE
            }
    }

    private fun resendEmail() {
        showProgressDialog()
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification()
                .addOnSuccessListener {
                    hideProgressDialog()
                    Toast
                        .makeText(activity, "Resent email verification", Toast.LENGTH_SHORT)
                        .show()

                    disableResendbutton()
                    timer.start()
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Toast
                        .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                        .show()

                    disableResendbutton()
                    timer.start()
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun reloadUser() {
        var firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.reload()
                .addOnSuccessListener {
                    firebaseUser = auth.currentUser
                    isEmailVerified(firebaseUser!!)
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun isEmailVerified(firebaseUser: FirebaseUser) {
        if (firebaseUser.isEmailVerified) {
            hideProgressDialog()
            timer.cancel()

            Snackbar
                .make(rootLayout, "Email verified", Snackbar.LENGTH_LONG)
                .show()

            dismiss()
        }
    }

    private fun setTimer() {
        timer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val message = "Resend in " + millisUntilFinished / 1000 + "s"
                binding.tvUpdateEmailResendEmail.text = message
            }
            override fun onFinish() {
                enableResendButton()
            }
        }
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
                try {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbUpdateEmail.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbUpdateEmail.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun enableResendButton() {
        binding.tvUpdateEmailResendEmail.setTextColor(ContextCompat.getColor(activity, R.color.green_primary))
        binding.tvUpdateEmailResendEmail.setText(R.string.resend_email_verification)
        binding.tvUpdateEmailResendEmail.isEnabled = true
    }

    private fun disableResendbutton() {
        binding.tvUpdateEmailResendEmail.setTextColor(ContextCompat.getColor(activity, R.color.darker_gray))
        binding.tvUpdateEmailResendEmail.isEnabled = false
    }
}