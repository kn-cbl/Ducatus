package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
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
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var currentAccountId: String
    private lateinit var categoryId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

        binding.tbTransactionDetail.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbTransactionDetail.inflateMenu(R.menu.edit_delete_menu)
        binding.tbTransactionDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    confirmDelete()
                    true
                }
                R.id.done -> {
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
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!

            val transactionId = intent.getStringExtra("transactionId").toString()
            val sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            loadTransaction(firebaseUser.uid, currentAccountId, transactionId)
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
                    categoryId = transaction.category_id.toString()

                    val formattedDate =
                        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                            .format(Date(transaction.transaction_date!!))

                    val meridian: String
                    val hour = (transaction.transaction_hour!! / 1000 / 60).toString().toInt()
                    val minute = (transaction.transaction_minute!! / 1000 / 60).toString().toInt()

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

                    binding.tfTransactionDetailCategory.editText?.setText(transaction.category_name)

                    if (transaction.subcategory_name != null) {
                        binding.tfTransactionDetailSubcategory.editText?.setText(transaction.subcategory_name)
                    }
                    else {
                        binding.tfTransactionDetailSubcategory.visibility = View.GONE
                    }

                    binding.tfTransactionDetailAmount.editText?.setText(transaction.transaction_amount.toInt().toString())

                    binding.tfTransactionDetailPaymentType.editText?.setText(transaction.transaction_payment_type)
                    binding.tfTransactionDetailNotes.editText?.setText(transaction.transaction_payment_type)
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

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_transaction))
            .setMessage(resources.getString(R.string.delete_transaction_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteTransaction() }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteTransaction() {
        showProgressDialogAction()
        databaseReference.removeValue()
            .addOnSuccessListener {
                updateBudget()
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateBudget() {
        databaseReference = database.getReference("budgets").child(firebaseUser.uid)
            .child(currentAccountId).child(categoryId).child("budget_amount_spent")

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                //val amountSpent = snapshot.value

            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clTransactionDetail, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun updateAccount() {

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