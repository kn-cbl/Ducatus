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
import com.ducatus.data.Account
import com.ducatus.databinding.FragmentResetUserDataDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class ResetUserDataDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentResetUserDataDialogBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var rootLayout: ConstraintLayout
    private val functions = 100 / 14

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clPrivacy)
        binding = FragmentResetUserDataDialogBinding.inflate(inflater, container, false)
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
            resetAccounts(firebaseUser.uid)
        }
        else {
            sessionExpired()
        }
    }

    private fun resetAccounts(uid: String) {
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        binding.pbResetData.setProgress(functions, true)

        val sharedPreferences = SharedPreferences(activity)
        val currentAccountId = sharedPreferences.accountId.toString()

        databaseReference = database.getReference("accounts").child(uid)
        databaseReference.child(currentAccountId).get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()
                if (account != null) {
                    account.remainingBudget = account.monthlyBudget
                    account.remainingBalance = account.monthlyBudget

                    val accountMap = mapOf(account.id to account)
                    databaseReference.setValue(accountMap)
                        .addOnSuccessListener {
                            deleteCategories(uid, currentAccountId)
                        }
                        .addOnFailureListener {
                            Snackbar
                                .make(rootLayout, it.localizedMessage!!, 5000)
                                .show()

                            dismiss()
                        }
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteCategories(uid: String, accountId: String) {
        binding.pbResetData.setProgress(functions * 2, true)
        databaseReference = database.getReference("categories").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                createDefaultCategories(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun createDefaultCategories(uid:String, accountId: String) {
        binding.pbResetData.setProgress(functions * 3, true)
        val keys = mutableListOf<String>()
        val size = AppResources().getCategoryItemCount()
        for (i in 0 until size) {
            val key = databaseReference.push().key
            keys.add(key!!)
        }

        val categories = AppResources().getCategories(keys)
        databaseReference.child(accountId).setValue(categories)
            .addOnSuccessListener {
                deleteSubcategories(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubcategories(uid: String) {
        binding.pbResetData.setProgress(functions * 4, true)
        databaseReference = database.getReference("subcategories").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteBudgets(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteBudgets(uid: String) {
        binding.pbResetData.setProgress(functions * 5, true)
        databaseReference = database.getReference("budgets").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteTransactions(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteTransactions(uid: String) {
        binding.pbResetData.setProgress(functions * 6, true)
        databaseReference = database.getReference("transactions").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubscriptions(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubscriptions(uid: String) {
        binding.pbResetData.setProgress(functions * 7, true)
        databaseReference = database.getReference("subscriptions").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubscriptionHistory(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubscriptionHistory(uid: String) {
        binding.pbResetData.setProgress(functions * 8, true)
        databaseReference = database.getReference("subscriptionHistory").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoans(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoans(uid: String) {
        binding.pbResetData.setProgress(functions * 9, true)
        databaseReference = database.getReference("loans").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoanHistory(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoanHistory(uid: String) {
        binding.pbResetData.setProgress(functions * 10, true)
        databaseReference = database.getReference("loanHistory").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoals(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoals(uid: String) {
        binding.pbResetData.setProgress(functions * 11, true)
        databaseReference = database.getReference("goals").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoalHistory(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoalHistory(uid: String) {
        binding.pbResetData.setProgress(functions * 12, true)
        databaseReference = database.getReference("goalHistory").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteChallenges(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteChallenges(uid: String) {
        binding.pbResetData.setProgress(functions * 13, true)
        databaseReference = database.getReference("challenges").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteNotifications(uid)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteNotifications(uid: String) {
        binding.pbResetData.setProgress(functions * 14, true)
        databaseReference = database.getReference("notifications").child(uid)
        databaseReference.removeValue()
            .addOnSuccessListener {
                try {
                    val intent = Intent(activity, HomeActivity::class.java)
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