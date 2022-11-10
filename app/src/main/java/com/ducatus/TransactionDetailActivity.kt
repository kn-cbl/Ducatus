package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import androidx.core.widget.doOnTextChanged
import com.ducatus.data.Transaction
import com.ducatus.databinding.ActivityTransactionDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityTransactionDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentAccountId: String
    private var firebaseUser: FirebaseUser? = null
    private var selectedTransaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        inputObserver()
        loadData()

        binding.tbTransactionDetail.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbTransactionDetail.inflateMenu(R.menu.edit_delete_menu)
        binding.tbTransactionDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    selectedTransaction?.let { confirmDelete(it) }
                    true
                }
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val transactionId = intent.getStringExtra("transactionId").toString()
            val sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            loadTransaction(firebaseUser!!.uid, currentAccountId, transactionId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadTransaction(uid: String, accountId: String, transactionId: String) {
        showProgressDialogMain()
        databaseReference = database.getReference("transactions").child(uid).child(accountId).child(transactionId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val transaction = snapshot.getValue<Transaction>()
                if (transaction != null) {
                    selectedTransaction = transaction

                    when (transaction.type) {
                        0 -> {
                            binding.rbTransactionIncome.isChecked = false
                            binding.rbTransactionExpense.isChecked = true
                        }
                        else -> {
                            binding.rbTransactionExpense.isChecked = false
                            binding.rbTransactionIncome.isChecked = true
                        }
                    }

                    val formattedDate =
                        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                            .format(Date(transaction.date!!))

                    val meridian: String
                    val hour = (transaction.hour!! / 1000 / 60).toString().toInt()
                    val minute = (transaction.minute!! / 1000 / 60).toString().toInt()

                    val formattedHour: Int
                    if (hour > 12) {
                        formattedHour = hour - 12
                        meridian = "PM"
                    }
                    else if (hour == 12) {
                        formattedHour = hour
                        meridian = "PM"
                    }
                    else if (hour == 0) {
                        formattedHour = hour + 12
                        meridian = "AM"
                    }
                    else { // < 12
                        formattedHour = hour
                        meridian = "AM"
                    }

                    val formattedMinute =
                        if (minute > 9) minute
                        else "0${minute}"

                    val time = "$formattedHour:$formattedMinute $meridian"

                    binding.tfTransactionDetailDate.editText?.setText(formattedDate)
                    binding.tfTransactionDetailTime.editText?.setText(time)

                    binding.tfTransactionDetailCategory.editText?.setText(transaction.categoryName)

                    if (transaction.subcategoryName != null) {
                        binding.tfTransactionDetailSubcategory.editText?.setText(transaction.subcategoryName)
                    }
                    else {
                        binding.tfTransactionDetailSubcategory.visibility = View.GONE
                    }

                    binding.tfTransactionDetailAmount.editText?.setText(transaction.amount.toInt().toString())

                    binding.tfTransactionDetailPaymentType.editText?.setText(transaction.paymentType)
                    binding.tfTransactionDetailNotes.editText?.setText(transaction.notes)
                }

                hideProgressDialogMain()
            }
            .addOnFailureListener {
                hideProgressDialogMain()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun inputObserver() {
        binding.tfTransactionDetailPaymentType.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) {
                binding.tfTransactionDetailPaymentType.error = getString(R.string.payment_type_empty)
            }
            else {
                binding.tfTransactionDetailPaymentType.error = null
            }
        }
    }

    private fun validateData() {
        val paymentType = binding.tfTransactionDetailPaymentType.editText?.text.toString().trim { it <= ' ' }
        val notes = binding.tfTransactionDetailNotes.editText?.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(paymentType)) {
            binding.tfTransactionDetailPaymentType.error = getString(R.string.payment_type_empty)
        }

        else {
            if (paymentType == selectedTransaction?.paymentType) {
                onBackPressed()
            }
            else {
                showProgressDialogAction()

                selectedTransaction?.paymentType = paymentType
                selectedTransaction?.notes = notes
                firebaseUser?.let { updateTransaction(it.uid, currentAccountId, selectedTransaction!!) }
            }
        }
    }

    private fun updateTransaction(uid: String, accountId: String, transaction: Transaction) {
        databaseReference = database.getReference("transactions").child(uid).child(accountId).child(transaction.id!!)
        databaseReference.setValue(selectedTransaction)
            .addOnSuccessListener {
                hideProgressDialogAction()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun confirmDelete(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_transaction))
            .setMessage(resources.getString(R.string.delete_transaction_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                firebaseUser?.let { deleteTransaction(it.uid, currentAccountId, transaction) }
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteTransaction(uid: String, accountId: String, transaction: Transaction) {
        showProgressDialogAction()
        databaseReference = database.getReference("transactions").child(uid).child(accountId).child(transaction.id!!)
        databaseReference.removeValue()
            .addOnSuccessListener {
                updateBudget(uid, accountId, transaction)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateBudget(uid: String, accountId: String, transaction: Transaction) {
        databaseReference = database.getReference("budgets").child(uid)
            .child(accountId).child(transaction.id!!).child("amountSpent")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val amountSpent = snapshot.value.toString().toDouble()
                val newAmount = when (transaction.type) {
                    0 -> amountSpent - transaction.amount
                    else -> amountSpent + transaction.amount
                }

                databaseReference.setValue(newAmount)
                    .addOnSuccessListener {
                        updateAccount(uid, accountId, transaction)
                    }
                    .addOnFailureListener {
                        hideProgressDialogAction()
                        Snackbar
                            .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateAccount(uid: String, accountId: String, transaction: Transaction) {
        databaseReference = database.getReference("accounts")
            .child(uid).child(accountId).child("remainingBalance")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val remainingBalance = snapshot.value.toString().toDouble()
                val newBalance = when (transaction.type) {
                    0 -> remainingBalance + transaction.amount
                    else -> remainingBalance - transaction.amount
                }

                databaseReference.setValue(newBalance)
                    .addOnSuccessListener {
                        hideProgressDialogAction()
                        onBackPressed()
                    }
                    .addOnFailureListener {
                        hideProgressDialogAction()
                        Snackbar
                            .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(binding.clTransactionDetail, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }.start()
    }

    private fun showProgressDialogMain() {
        binding.pbTransactionDetailMain.visibility = View.VISIBLE
        binding.svTransactionDetail.visibility = View.GONE
    }

    private fun hideProgressDialogMain() {
        binding.pbTransactionDetailMain.visibility = View.INVISIBLE
        binding.svTransactionDetail.visibility = View.VISIBLE
    }

    private fun showProgressDialogAction() {
        binding.pbTransactionDetail.visibility = View.VISIBLE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgressDialogAction() {
        binding.pbTransactionDetail.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}