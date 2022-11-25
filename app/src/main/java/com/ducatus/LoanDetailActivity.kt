package com.ducatus

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.LoanHistoryAdapter
import com.ducatus.data.Account
import com.ducatus.data.Loan
import com.ducatus.data.LoanHistory
import com.ducatus.databinding.ActivityLoanDetailBinding
import com.ducatus.interfaces.LoanHistoryInterface
import com.ducatus.viewmodel.LoanViewModel
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
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LoanDetailActivity : AppCompatActivity(), LoanHistoryInterface {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoanDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentAccountId: String
    private lateinit var selectedLoan: Loan
    private lateinit var sharedPreferences: SharedPreferences
    private var firebaseUser: FirebaseUser? = null
    private val loanViewModel: LoanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoanDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

        binding.tbLoanDetail.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tbLoanDetail.inflateMenu(R.menu.edit_delete_menu)
        binding.tbLoanDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    firebaseUser?.let {
                        val bundle = Bundle()
                        bundle.putString("loan", Gson().toJson(selectedLoan))

                        val fragmentManager = supportFragmentManager
                        val newFragment = LoanEditDialogFragment()
                        newFragment.arguments = bundle
                        newFragment.show(fragmentManager, "dialog")
                    }

                    true
                }
                R.id.delete -> {
                    firebaseUser?.let {
                        confirmDelete(
                            it.uid,
                            currentAccountId,
                            selectedLoan.id!!,
                            selectedLoan.notificationId!!,
                        )
                    }
                    true
                }
                else -> false
            }
        }

        loanViewModel.loan.observe(this) { loan ->
            loan.getContentIfNotHandled()?.let { content ->
                firebaseUser?.let {
                    selectedLoan = content
                    loadLoan(it.uid, currentAccountId, selectedLoan.id!!)
                }
            }
        }

        binding.btnLoanDetailLend.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("loan", Gson().toJson(selectedLoan))

            val fragmentManager = supportFragmentManager
            val newFragment = LoanLendDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }

        binding.btnLoanDetailBorrow.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("loan", Gson().toJson(selectedLoan))

            val fragmentManager = supportFragmentManager
            val newFragment = LoanBorrowDialogFragment()
            newFragment.arguments = bundle
            newFragment.show(fragmentManager, "dialog")
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun getActivityInterface(): Activity {
        return this
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            val loanId = intent.getStringExtra("loanId")!!

            sharedPreferences = SharedPreferences(this)
            currentAccountId = sharedPreferences.accountId.toString()
            val accountId = intent.getStringExtra("accountId")

            // check if accountId from the notification is the same as current accountId
            // this prevents checking an item designated for a different account
            if (accountId != null && currentAccountId != accountId) {
                deselectAccount(firebaseUser!!.uid, currentAccountId, accountId, loanId)
            }
            else {
                loadLoan(firebaseUser!!.uid, currentAccountId, loanId)
            }
        }
        else {
            sessionExpired()
        }
    }

    private fun deselectAccount(uid: String, currentAccountId: String, selectedAccountId: String, loanId: String) {
        databaseReference = database.getReference("accounts").child(uid)
        databaseReference.child(currentAccountId).child("selected").setValue(false)
            .addOnSuccessListener {
                selectAccount(uid, selectedAccountId, loanId)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clLoanDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun selectAccount(uid: String, accountId: String, loanId: String) {
        databaseReference.child(accountId).get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()

                databaseReference.child(accountId).child("selected").setValue(true)
                    .addOnSuccessListener {
                        sharedPreferences.accountId = accountId
                        sharedPreferences.accountName = account?.name
                        sharedPreferences.accountColor = account?.color

                        currentAccountId = sharedPreferences.accountId.toString()
                        loadLoan(uid, currentAccountId, loanId)
                    }
                    .addOnFailureListener {
                        hideProgressDialog()
                        Snackbar
                            .make(binding.clLoanDetail, it.localizedMessage!!,5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clLoanDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun loadLoan(uid: String, accountId: String, loanId: String) {
        showProgressDialog()
        databaseReference =
            database.getReference("loans")
                .child(uid)
                .child(accountId)
                .child(loanId)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.loan_empty_title))
                        .setPositiveButton(resources.getString(R.string.go_back)) { _, _ -> onBackPressed() }
                        .setOnDismissListener { onBackPressed() }
                        .show()
                }
                else {
                    val loan = snapshot.getValue<Loan>()
                    if (loan != null) {
                        loadLoanHistory(uid, currentAccountId, loanId)
                        selectedLoan = loan

                        binding.tvLoanDetailIcon.text = loan.name?.get(0)?.uppercase()
                        binding.tvLoanDetailName.text = loan.name

                        val notesText =
                            if (loan.notes == null) "No notes"
                            else loan.notes

                        binding.tvLoanDetailNotes.text = notesText

                        // lend or borrow or completed
                        val loanState =
                            if (loan.paidAt != null) {
                                "darker_gray"
                            }
                            else {
                                when (loan.type) {
                                    "L" -> "bright_red"
                                    "B" -> "green_secondary"
                                    else -> "darker_gray"
                                }
                            }

                        val amountColorRes = resources.getIdentifier(
                            loanState,
                            "color",
                            packageName
                        )

                        val amountText = "₱" + String.format("%,.2f", loan.amount)
                        binding.tvLoanDetailAmount.text = amountText
                        binding.tvLoanDetailAmount.setTextColor(
                            ContextCompat.getColor(this, amountColorRes)
                        )

                        if (loan.paidAt != null) {
                            // hide next payment info if loan is paid
                            binding.llEndInfo.visibility = View.INVISIBLE

                            // change button text if loan is paid
                            binding.btnLoanDetailLend.text = getString(R.string.lend_money)
                            binding.btnLoanDetailBorrow.text = getString(R.string.borrow_money)
                        }
                        else {
                            binding.llEndInfo.visibility = View.VISIBLE

                            val zdt = ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(loan.dueDate!!),
                                ZoneId.systemDefault()
                            )
                            val zdtToday = ZonedDateTime.ofInstant(
                                Instant.now(),
                                ZoneId.systemDefault()
                            )
                            val startEpoch = zdt.toInstant().toEpochMilli()
                            val endEpoch = zdtToday.toInstant().toEpochMilli()

                            val overdueText =
                                if (startEpoch < endEpoch) getElapsedTime(loan.dueDate!!)
                                else null

                            overdueText?.let {
                                binding.tvLoanDetailOverdue.text = overdueText
                                binding.tvLoanDetailOverdue.visibility = View.VISIBLE
                            }

                            val dtfMonthDay = DateTimeFormatter.ofPattern("MM/dd/yy")
                            val formattedDate = dtfMonthDay.format(zdt)

                            val dateText = "Due on $formattedDate"
                            binding.tvLoanDetailDate.text = dateText

                            val dtfHourMinute = DateTimeFormatter.ofPattern("h:mm a")
                            val formattedTime = dtfHourMinute.format(zdt)
                            binding.tvLoanDetailTime.text = formattedTime

                            // change button text according to type if loan is still not paid
                            when (selectedLoan.type) {
                                "B" -> {
                                    binding.btnLoanDetailLend.text = getString(R.string.pay_loan)
                                    binding.btnLoanDetailLend.backgroundTintList =
                                        ContextCompat.getColorStateList(this, R.color.green_primary)

                                    binding.btnLoanDetailBorrow.visibility = View.GONE
                                }
                                "L" -> {
                                    binding.btnLoanDetailBorrow.text = getString(R.string.receive_payment)
                                    binding.btnLoanDetailBorrow.backgroundTintList =
                                        ContextCompat.getColorStateList(this, R.color.green_primary)

                                    binding.btnLoanDetailLend.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clLoanDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun getElapsedTime(dueDate: Long): String {
        val startDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(dueDate),
            ZoneId.systemDefault()
        )
        val endDate = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val elapsedDays = ChronoUnit.DAYS.between(startDate, endDate)
        val elapsedHours = ChronoUnit.HOURS.between(startDate, endDate)
        val elapsedMinutes = ChronoUnit.MINUTES.between(startDate, endDate)
        val elapsedSeconds = ChronoUnit.SECONDS.between(startDate, endDate)

        val dateText =
            if (elapsedDays > 0) {
                if (elapsedDays.toInt() == 1) {
                    "${elapsedDays}d overdue"
                }
                else {
                    "${elapsedDays}d overdue"
                }
            }
            else if (elapsedHours > 0) {
                if (elapsedHours.toInt() == 1) {
                    "${elapsedHours}h overdue"
                }
                else {
                    "${elapsedHours}h overdue"
                }
            }
            else if (elapsedMinutes > 0) {
                if (elapsedMinutes.toInt() == 1) {
                    "${elapsedMinutes}m overdue"
                }
                else {
                    "${elapsedMinutes}m overdue"
                }
            }
            else {
                if (elapsedSeconds.toInt() == 1) {
                    "${elapsedSeconds}s overdue"
                }
                else {
                    "${elapsedSeconds}s overdue"
                }
            }

        return dateText
    }

    private fun loadLoanHistory(uid: String, accountId: String, loanId: String) {
        databaseReference =
            database
                .getReference("loanHistory")
                .child(uid)
                .child(accountId)
                .child(loanId)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val loanHistoryAdapter = LoanHistoryAdapter(mutableListOf(), this)
                binding.rvLoanHistory.adapter = loanHistoryAdapter
                binding.rvLoanHistory.layoutManager = LinearLayoutManager(this)

                val loansHistory = mutableListOf<LoanHistory>()
                for (child in snapshot.children) {
                    val loanHistory  = child.getValue<LoanHistory>()
                    if (loanHistory != null) {
                        loansHistory.add(loanHistory)
                    }
                }

                // sort by latest date
                loansHistory.sortByDescending { it.date }

                for (loanHistory in loansHistory) {
                    loanHistoryAdapter.addLoanHistory(loanHistory)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.clLoanDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun confirmDelete(uid: String, accountId: String, loanId: String, notificationId: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_loan_title))
            .setMessage(resources.getString(R.string.delete_loan_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                deleteLoan(uid, accountId, loanId, notificationId)
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> } // do nothing
            .show()
    }

    private fun deleteLoan(uid: String, accountId: String, loanId: String, notificationId: Int) {
        showProgressDialog()
        databaseReference =
            database.getReference("loans")
                .child(uid)
                .child(accountId)
                .child(loanId)

        databaseReference.removeValue()
            .addOnSuccessListener {
                cancelNotification(this, notificationId)
                deleteLoanHistory(uid, accountId, loanId)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clLoanDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun deleteLoanHistory(uid: String, accountId: String, loanId: String) {
        databaseReference =
            database.getReference("loanHistory")
                .child(uid)
                .child(accountId)
                .child(loanId)

        databaseReference.removeValue()
            .addOnSuccessListener {
                hideProgressDialog()
                onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clLoanDetail, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.loansChannelId)
        if (notificationChannel != null) {
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            notificationIntent.action = "com.ducatus.LOAN"

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun showProgressDialog() {
        binding.pbLoanDetail.visibility = View.VISIBLE
        binding.llLoanDetail.visibility = View.GONE
        binding.rvLoanHistory.visibility = View.GONE
        binding.llLoanDetailButtons.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbLoanDetail.visibility = View.GONE
        binding.llLoanDetail.visibility = View.VISIBLE
        binding.rvLoanHistory.visibility = View.VISIBLE
        binding.llLoanDetailButtons.visibility = View.VISIBLE
    }
}