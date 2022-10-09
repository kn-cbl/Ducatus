package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUpdateEmailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UpdateEmailFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var binding: FragmentUpdateEmailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var email: String
    private lateinit var rootLayout: LinearLayout
    private var emailRegex = "^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+\$"
    private var status: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentUpdateEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootLayout = activity.findViewById(R.id.llUserProfile)
        inputObserver()

        binding.btnUpdateEmail.setOnClickListener {
            // validate credentials -> show confirmation -> reauthenticate -> update email -> send email verification
            validateCredentials()
        }

        binding.tvResendEmailVerification.setOnClickListener {
            resendEmail()
        }
    }

    override fun onResume() {
        super.onResume()
        if (status) reloadUser()
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
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
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
            .setTitle(resources.getString(R.string.change_email_mark))
            .setPositiveButton(resources.getString(R.string.change)) { _, _ -> reauthenticateUser(firebaseUser, email, password) }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            .show()
    }

    private fun reauthenticateUser(firebaseUser: FirebaseUser, email: String, password: String) {
        showProgressDialog()
        val credential = EmailAuthProvider.getCredential(firebaseUser.email.toString(), password)
        firebaseUser.reauthenticate(credential)
            .addOnSuccessListener {
                updateEmail(firebaseUser, email)
            }
            .addOnFailureListener {
                hideProgressDialog()
                binding.tfUpdateEmailReauthenticate.error = it.localizedMessage
            }
    }

    private fun updateEmail(firebaseUser: FirebaseUser, email: String) {
        showProgressDialog()
        firebaseUser.updateEmail(email)
            .addOnSuccessListener {
                unlinkGoogle(firebaseUser, email)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to update email, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateEmail(firebaseUser, email) }
                    .show()
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
                    unlinkGoogle(firebaseUser, email)
                }
        }
        else {
            updateDB(firebaseUser, email)
        }
    }

    private fun updateDB(firebaseUser: FirebaseUser, email: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("users").child(firebaseUser.uid).child("email")
        databaseReference.setValue(email)
            .addOnSuccessListener {
                sendEmail(firebaseUser)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to update email, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateDB(firebaseUser, email) }
                    .show()
            }
    }

    private fun sendEmail(firebaseUser: FirebaseUser) {
        showProgressDialog()
        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                binding.tvResendEmailVerification.setTextColor(ContextCompat.getColor(activity,R.color.darker_gray))
                binding.tvResendEmailVerification.isEnabled = false
                binding.tvResendEmailVerification.visibility = View.VISIBLE
                status = true

                hideProgressDialog()
                startTimer(false)

                Snackbar
                    .make(rootLayout, "Successfully updated email, email verification has been sent", Snackbar.LENGTH_LONG)
                    .show()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to send email verification, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Resend") { sendEmail(firebaseUser) }
                    .show()
            }
    }

    private fun resendEmail() {
        showProgressDialog()
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification()
                .addOnSuccessListener {
                    binding.tvResendEmailVerification.setTextColor(ContextCompat.getColor(activity,R.color.darker_gray))
                    binding.tvResendEmailVerification.isEnabled = false

                    hideProgressDialog()
                    startTimer(false)
                    Snackbar
                        .make(rootLayout, "Resent email verification", Snackbar.LENGTH_LONG)
                        .show()
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "Unable to resend email verification, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Resend") { resendEmail() }
                        .show()
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
            startTimer(true)

            Snackbar
                .make(rootLayout, "Successfully verified email", Snackbar.LENGTH_LONG)
                .show()

            // add 3 second delay
            object : CountDownTimer(3000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    // do nothing
                }
                override fun onFinish() {
                    try {
                        val action = UpdateEmailFragmentDirections.actionUpdateEmailFragmentToUserProfileFragment()
                        findNavController().navigate(action)
                    }
                    catch (e: Exception) {}
                }
            }.start()
        }
    }

    private fun startTimer(finish: Boolean) {
        val timer: CountDownTimer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val message = "Resend in " + millisUntilFinished / 1000
                binding.tvResendEmailVerification.text = message
            }
            override fun onFinish() {
                binding.tvResendEmailVerification.setTextColor(ContextCompat.getColor(activity,R.color.green_primary))
                binding.tvResendEmailVerification.setText(R.string.resend_email_verification)
                binding.tvResendEmailVerification.isEnabled = true
            }
        }

        if (finish) timer.cancel()
        else timer.start()
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
        binding.btnUpdateEmail.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.gray)
        binding.pbUpdateEmail.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnUpdateEmail.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        binding.pbUpdateEmail.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}