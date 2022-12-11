package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentDeleteAccountDialogBinding
import com.ducatus.viewmodel.UpdateViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DeleteAccountDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentDeleteAccountDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private val args: DeleteAccountDialogFragmentArgs by navArgs()
    private val updateViewModel: UpdateViewModel by activityViewModels()
    private val functions = 100 / 13

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        binding = FragmentDeleteAccountDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            deleteAccount(firebaseUser.uid, args.accountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun deleteAccount(uid: String, accountId: String) {
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        binding.pbDeleteAccount.setProgress(functions, true)
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteCategories(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteCategories(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 2, true)
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubcategories(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubcategories(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 3, true)
        databaseReference = database.getReference("subcategories").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteBudgets(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteBudgets(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 4, true)
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteTransactions(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteTransactions(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 5, true)
        databaseReference = database.getReference("transactions").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubscriptions(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubscriptions(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 6, true)
        databaseReference = database.getReference("subscriptions").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubscriptionHistory(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubscriptionHistory(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 7, true)
        databaseReference = database.getReference("subscriptionHistory").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoans(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoans(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 8, true)
        databaseReference = database.getReference("loans").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoanHistory(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoanHistory(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 9, true)
        databaseReference = database.getReference("loanHistory").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoals(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoals(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 10, true)
        databaseReference = database.getReference("goals").child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoalHistory(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoalHistory(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 11, true)
        databaseReference = database.getReference("goalHistory").child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteChallengeHistory(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteChallengeHistory(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 12, true)
        databaseReference = database.getReference("challengeHistory").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteNotifications(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteNotifications(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 13, true)
        databaseReference = database.getReference("notifications").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                updateViewModel.update(true)
                dismiss()
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