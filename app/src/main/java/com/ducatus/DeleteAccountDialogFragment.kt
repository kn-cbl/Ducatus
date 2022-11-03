package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentDeleteAccountDialogBinding
import com.ducatus.viewmodel.AccountViewModel
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
    private val viewModel: AccountViewModel by activityViewModels()
    private val functions = 100 / 10

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
        disableWindow()
        binding.pbDeleteAccount.setProgress(functions, true)
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteBudgets(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteBudgets(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 2, true)
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteCategories(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteCategories(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 3, true)
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteSubcategories(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteSubcategories(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 4, true)
        databaseReference = database.getReference("subcategories").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteChallenges(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteChallenges(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 5, true)
        databaseReference = database.getReference("challenges").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteGoals(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteGoals(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 6, true)
        databaseReference = database.getReference("goals").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteLoans(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteLoans(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 7, true)
        databaseReference = database.getReference("loans").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deletePlannedPayments(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deletePlannedPayments(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 8, true)
        databaseReference = database.getReference("planned_payments").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteReports(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteReports(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 9, true)
        databaseReference = database.getReference("reports").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                deleteTransactions(uid, accountId)
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()

                dismiss()
            }
    }

    private fun deleteTransactions(uid: String, accountId: String) {
        binding.pbDeleteAccount.setProgress(functions * 10, true)
        databaseReference = database.getReference("transactions").child(uid).child(accountId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                enableWindow()
                viewModel.update(true)
                dismiss()
            }
            .addOnFailureListener {
                enableWindow()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
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

    private fun enableWindow() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun disableWindow() {
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}