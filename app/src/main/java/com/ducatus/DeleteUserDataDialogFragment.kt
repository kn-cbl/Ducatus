package com.ducatus

import android.app.Activity
import android.content.Context
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentDeleteUserDataDialogBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DeleteUserDataDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentDeleteUserDataDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private lateinit var rootLayout: ConstraintLayout
    private val functions = 100 / 11
    private val args: DeleteUserDataDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clPrivacy)

        binding = FragmentDeleteUserDataDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()
        loadData()

        binding.btnDeleteUserCancel.setOnClickListener {
            dismiss()
        }

        binding.btnDeleteUserConfirm.setOnClickListener {
            validateCredentials()
        }
    }

    private fun loadData() {
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!
            database = Firebase.database
            isGoogleOnly(firebaseUser)
        }
    }

    private fun isGoogleOnly(firebaseUser: FirebaseUser) {
        disableWindow()
        binding.llDeleteUserReauthenticate.visibility = View.GONE
        binding.llDeleteUser.visibility = View.VISIBLE

        val providers = mutableListOf<String>()
        for (item in firebaseUser.providerData) {
            providers.add(item.providerId)
        }

        if (!providers.contains("password")) {
            if (args.id == "delete app user") {
                getGoogleIdToken()
            }
        }
        else {
            enableWindow()
            binding.llDeleteUserReauthenticate.visibility = View.VISIBLE
            binding.llDeleteUser.visibility = View.GONE
        }
    }

    private fun getGoogleIdToken() {
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
            if (e.statusCode == 12500) {
                enableWindow()
                Snackbar
                    .make(rootLayout, getString(R.string.google_sign_in_failed), 5000)
                    .show()

                dismiss()
            }
        }
    }

    private fun reauthenticateGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseUser.reauthenticate(credential)
            .addOnSuccessListener {
                gsc.signOut()
                deleteUser(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun inputObserver() {
        binding.tfDeleteUserEmail.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfDeleteUserEmail.error = getString(R.string.email_empty)
            else binding.tfDeleteUserEmail.error = null
        }
        binding.tfDeleteUserPassword.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfDeleteUserPassword.error = getString(R.string.password_empty)
            else binding.tfDeleteUserPassword.error = null
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

        val email = binding.tfDeleteUserEmail.editText?.text.toString().trim {it <= ' '}
        val password = binding.tfDeleteUserPassword.editText?.text.toString().trim {it <= ' '}

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(email)) binding.tfDeleteUserEmail.error = getString(R.string.email_empty)
            if (TextUtils.isEmpty(password)) binding.tfDeleteUserPassword.error = getString(R.string.password_empty)
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                reauthenticatePassword(firebaseUser, email, password)
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun reauthenticatePassword(firebaseUser: FirebaseUser, email: String, password: String) {
        showProgressDialog()
        val credential = EmailAuthProvider.getCredential(email, password)
        firebaseUser.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.llDeleteUserReauthenticate.visibility = View.GONE
                    binding.llDeleteUser.visibility = View.VISIBLE
                    deleteUser(firebaseUser)
                }
                else {
                    hideProgressDialog()
                    val exception = task.exception as FirebaseAuthException
                    when (exception.errorCode) {
                        "ERROR_WRONG_PASSWORD" -> binding.tfDeleteUserPassword.error = getString(R.string.password_invalid)
                        else -> binding.tfDeleteUserPassword.error = exception.localizedMessage
                    }
                }
            }
    }

    private fun deleteUser(firebaseUser: FirebaseUser) {
        disableWindow()
        binding.pbDeleteUser.setProgress(functions, true)
        databaseReference = database.getReference("users").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteAccounts(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteAccounts(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 2, true)
        databaseReference = database.getReference("accounts").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteCategories(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteCategories(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 3, true)
        databaseReference = database.getReference("categories").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteBudgets(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteBudgets(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 4, true)
        databaseReference = database.getReference("budgets").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteTransactions(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteTransactions(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 5, true)
        databaseReference = database.getReference("transactions").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deletePlannedPayments(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deletePlannedPayments(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 6, true)
        databaseReference = database.getReference("planned_payments").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoans(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoans(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 7, true)
        databaseReference = database.getReference("loans").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoals(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoals(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 8, true)
        databaseReference = database.getReference("goals").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteChallenges(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteChallenges(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 9, true)
        databaseReference = database.getReference("challenges").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteReports(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteReports(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 10, true)
        databaseReference = database.getReference("reports").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteFirebaseUser(firebaseUser)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteFirebaseUser(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 11, true)
        firebaseUser.delete()
            .addOnSuccessListener {
                try {
                    val intent = Intent(activity, PostDeleteActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun sessionExpired() {
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
        binding.pbDeleteUserReauthenticate.visibility = View.VISIBLE
        disableWindow()
    }

    private fun hideProgressDialog() {
        binding.pbDeleteUserReauthenticate.visibility = View.INVISIBLE
        enableWindow()
    }

    private fun enableWindow() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}