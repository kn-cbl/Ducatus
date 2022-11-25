package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.ducatus.databinding.FragmentDeleteUserDataDialogBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DeleteUserDataDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentDeleteUserDataDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private lateinit var rootLayout: ConstraintLayout
    private val functions = 100 / 15

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
        loadData()
    }

    private fun loadData() {
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!
            database = Firebase.database
            isGoogleOnly(firebaseUser)
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
            gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            gsc = GoogleSignIn.getClient(activity, gso)
            gsc.signOut()
        }

        deleteUser(firebaseUser)
    }

    private fun deleteUser(firebaseUser: FirebaseUser) {
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        binding.pbDeleteUser.setProgress(functions, true)
        databaseReference = database.getReference("users").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteAccounts(firebaseUser)
            }
            .addOnFailureListener {
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
                deleteSubcategories(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubcategories(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 4, true)
        databaseReference = database.getReference("subcategories").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteBudgets(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteBudgets(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 5, true)
        databaseReference = database.getReference("budgets").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteTransactions(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteTransactions(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 6, true)
        databaseReference = database.getReference("transactions").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubscriptions(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubscriptions(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 7, true)
        databaseReference = database.getReference("subscriptions").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubscriptionHistory(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubscriptionHistory(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 8, true)
        databaseReference = database.getReference("subscriptionHistory").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoans(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoans(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 9, true)
        databaseReference = database.getReference("loans").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoanHistory(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoanHistory(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 10, true)
        databaseReference = database.getReference("loanHistory").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoals(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoals(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 11, true)
        databaseReference = database.getReference("goals").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoalHistory(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoalHistory(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 12, true)
        databaseReference = database.getReference("goalHistory").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteChallenges(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteChallenges(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 13, true)
        databaseReference = database.getReference("challenges").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteNotifications(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteNotifications(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 14, true)
        databaseReference = database.getReference("notifications").child(firebaseUser.uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteFirebaseUser(firebaseUser)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteFirebaseUser(firebaseUser: FirebaseUser) {
        binding.pbDeleteUser.setProgress(functions * 15, true)
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
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
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
}