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
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"

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
            hideProgressDialog()
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
                    updateEmail(firebaseUser, email)
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

    private fun updateEmail(firebaseUser: FirebaseUser, email: String) {
        firebaseUser.verifyBeforeUpdateEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast
                        .makeText(activity, "Verification email has been sent to ${firebaseUser.email}", Toast.LENGTH_LONG)
                        .show()

                    unlinkGoogle(firebaseUser, email)
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
                    updateDB(firebaseUser, email)
                }
                .addOnFailureListener {
                    binding.tfUpdateEmail.error = it.localizedMessage
                }
        }
        else {
            updateDB(firebaseUser, email)
        }
    }

    private fun updateDB(firebaseUser: FirebaseUser, email: String) {
        database = Firebase.database
        databaseReference = database.getReference("users").child(firebaseUser.uid).child("email")
        databaseReference.setValue(email)
            .addOnSuccessListener {
                hideProgressDialog()
                dismiss()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast
                    .makeText(activity, it.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity.finish()
        }

        dialog.show()
    }

    private fun showProgressDialog() {
        binding.pbUpdateEmail.visibility = View.VISIBLE
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialog() {
        binding.pbUpdateEmail.visibility = View.INVISIBLE
        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}