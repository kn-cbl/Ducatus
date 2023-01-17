package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentDeleteUserAccountBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class DeleteUserAccountFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentDeleteUserAccountBinding
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var accountType: String
    private var firebaseUser: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clPrivacy)
        binding = FragmentDeleteUserAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        binding.btnDeleteUserAccountReauthenticate.setOnClickListener {
            firebaseUser?.let {
                when (accountType) {
                    "google" -> getGoogleIdToken()
                    "password" -> validateCredentials()
                }
            }
        }

        binding.btnDeleteUserAccountCancel.setOnClickListener {
            activity.onBackPressed()
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            isGoogleOnly(firebaseUser!!)
        }
        else {
            sessionExpired()
        }
    }

    private fun isGoogleOnly(firebaseUser: FirebaseUser) {
        val providers = mutableListOf<String>()
        for (item in firebaseUser.providerData) {
            providers.add(item.providerId)
        }

        if (!providers.contains("password")) {
            accountType = "google"
            binding.mdDeleteUserAccount.visibility = View.GONE
            binding.tvDeleteUserAccountTitle.visibility = View.GONE
            binding.tfDeleteUserAccountEmail.visibility = View.GONE
            binding.tfDeleteUserAccountPassword.visibility = View.GONE
        }
        else {
            accountType = "password"
        }
    }

    private fun getGoogleIdToken() {
        showProgressDialog()
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        gsc = GoogleSignIn.getClient(activity, gso)
        gsc.silentSignIn()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleSignInResult(task)
                }
                else {
                    val signInIntent: Intent = gsc.signInIntent
                    startActivityForResult(signInIntent, 100)
                }
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val googleSignInAccount: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            if (googleSignInAccount != null) {
                val idToken = googleSignInAccount.idToken
                if (idToken != null) {
                    reauthenticateGoogle(idToken)
                }
            }
        }
        catch (e: ApiException) {
            hideProgressDialog()
            if (e.statusCode == 12500) {
                Snackbar
                    .make(rootLayout, getString(R.string.google_sign_in_failed), 5000)
                    .show()
            }
        }
    }

    private fun reauthenticateGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseUser?.reauthenticate(credential)
            ?.addOnSuccessListener {
                try {
                    val action = DeleteUserAccountFragmentDirections.actionDeleteUserAccountFragmentToDeleteUserAccountConfirmFragment()
                    findNavController().navigate(action)
                }
                catch (e: Exception) {}
            }
            ?.addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun inputObserver() {
        binding.tfDeleteUserAccountEmail.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfDeleteUserAccountEmail.error = getString(R.string.email_empty)
            else binding.tfDeleteUserAccountEmail.error = null
        }
        binding.tfDeleteUserAccountPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfDeleteUserAccountPassword.error = getString(R.string.password_empty)
            else binding.tfDeleteUserAccountPassword.error = null
        }
    }

    private fun validateCredentials() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val email = binding.tfDeleteUserAccountEmail.editText?.text.toString().trim { it <= ' '}
        val password = binding.tfDeleteUserAccountPassword.editText?.text.toString().trim { it <= ' '}

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(email)) binding.tfDeleteUserAccountEmail.error = getString(R.string.email_empty)
            if (TextUtils.isEmpty(password)) binding.tfDeleteUserAccountPassword.error = getString(R.string.password_empty)
        }
        else {
            firebaseUser?.let { reauthenticatePassword(it, email, password)}
        }
    }

    private fun reauthenticatePassword(firebaseUser: FirebaseUser, email: String, password: String) {
        showProgressDialog()
        val credential = EmailAuthProvider.getCredential(email, password)
        firebaseUser.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    try {
                        val action = DeleteUserAccountFragmentDirections.actionDeleteUserAccountFragmentToDeleteUserAccountConfirmFragment()
                        findNavController().navigate(action)
                    }
                    catch (e: Exception) {}
                }
                else {
                    hideProgressDialog()
                    task.exception?.let {
                        try {
                            throw it
                        }
                        catch (exception: FirebaseAuthException) {
                            when (exception.errorCode) {
                                "ERROR_WRONG_PASSWORD" -> binding.tfDeleteUserAccountPassword.error = getString(R.string.password_invalid)
                                else -> binding.tfDeleteUserAccountPassword.error = exception.localizedMessage
                            }
                        }
                        catch (exception: FirebaseTooManyRequestsException) {
                            binding.tfDeleteUserAccountPassword.error = exception.localizedMessage
                        }
                        catch (exception: Exception) {
                            binding.tfDeleteUserAccountPassword.error = exception.localizedMessage
                        }
                    }
                }
            }
    }

    private fun showProgressDialog() {
        binding.pbDeleteUserAccount.visibility = View.VISIBLE
        binding.btnDeleteUserAccountReauthenticate.isClickable = false
        binding.btnDeleteUserAccountReauthenticate.text = null
        binding.btnDeleteUserAccountReauthenticate.backgroundTintList =
            ContextCompat.getColorStateList(activity, R.color.gray)
    }

    private fun hideProgressDialog() {
        binding.pbDeleteUserAccount.visibility = View.INVISIBLE
        binding.btnDeleteUserAccountReauthenticate.isClickable = true
        binding.btnDeleteUserAccountReauthenticate.text = getString(R.string.delete_my_account)
        binding.btnDeleteUserAccountReauthenticate.backgroundTintList =
            ContextCompat.getColorStateList(activity, R.color.darker_red)
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
}