package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.data.Transaction
import com.ducatus.data.TransactionGroup
import com.ducatus.databinding.FragmentTransactionsBreakdownBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.util.*

class TransactionsBreakdownFragment : Fragment(), TransactionInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentTransactionsBreakdownBinding
    private lateinit var currentAccountId: String
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var rootLayout: DrawerLayout
    private lateinit var transactionsListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)

        auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!
            database = Firebase.database

            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()
        }
        else {
            sessionExpired()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionsBreakdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val lastWeek = calendar.timeInMillis

        showProgressDialog()
        setTransactionsListener(lastWeek)
        databaseReference = database.getReference("transactions").child(firebaseUser.uid).child(currentAccountId)
        databaseReference.addValueEventListener(transactionsListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDatePicker()

        // special case, show date picker when clicked
        binding.rbTransactionsBreakdownCalendar.setOnClickListener {
            try {
                datePicker.show(childFragmentManager, "tag")
            }
            catch (e: Exception) {}
        }

        binding.rgTransactionsBreakdown.setOnCheckedChangeListener { _, checkedId ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

            if (checkedId != R.id.rbTransactionsBreakdownCalendar) {
                when (checkedId) {
                    R.id.rbTransactionsBreakdownWeek -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                    }
                    R.id.rbTransactionsBreakdownMonth -> {
                        calendar.add(Calendar.MONTH, -1)
                    }
                    R.id.rbTransactionsBreakdownYear -> {
                        calendar.add(Calendar.YEAR, -1)
                    }
                }

                val date = calendar.timeInMillis
//            val formattedDate =
//                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
//                    .format(Date(date))
//
//            Snackbar.make(rootLayout, formattedDate, 3000).show()

                // remove listener before adding new listener based on selected date
                showProgressDialog()
                databaseReference.removeEventListener(transactionsListener)
                setTransactionsListener(date)
                databaseReference.addValueEventListener(transactionsListener)
            }
        }

        binding.fabAddTransaction.setOnClickListener {
            val intent = Intent(activity, TransactionAddActivity::class.java)
            startActivity(intent)
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onStop() {
        super.onStop()
        databaseReference.removeEventListener(transactionsListener)
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem(transactionId: String) {
        val intent = Intent(activity, TransactionDetailActivity::class.java)
        intent.putExtra("transactionId", transactionId)
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun setDatePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        calendar.timeInMillis = today
        calendar[Calendar.MONTH] = Calendar.JANUARY
        calendar[Calendar.YEAR] = Calendar.YEAR - 5
        val startDate = calendar.timeInMillis

        // Build constraints.
        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setStart(startDate)
                .setEnd(today)

        datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { date ->
            // remove listener before adding new listener based on selected date
            showProgressDialog()
            databaseReference.removeEventListener(transactionsListener)
            setTransactionsListener(date)
            databaseReference.addValueEventListener(transactionsListener)
        }
    }

    private fun setTransactionsListener(date: Long) {
        transactionsListener = object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionGroupAdapter = TransactionGroupAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                binding.rvTransactionsBreakdown.adapter = transactionGroupAdapter
                binding.rvTransactionsBreakdown.layoutManager = LinearLayoutManager(activity)

                val transactions = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        if (transaction.transaction_date!! >= date) {
                            transactions.add(transaction)
                        }
                    }
                }

                // sort by date
                transactions.sortByDescending {
                    it.transaction_date!! + it.transaction_hour!! + it.transaction_minute!!
                }

                var transactionAdapter: TransactionAdapter
                var group: TransactionGroup

                val newTransactions = mutableListOf<Transaction>()
                var totalAmount = 0.0

                if (transactions.size == 1) {
                    newTransactions.add(transactions[0])
                    totalAmount += determineTransactionType(
                        transactions[0].transaction_type,
                        transactions[0].transaction_amount
                    )

                    transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                    group = TransactionGroup(transactions[0].transaction_date, totalAmount, transactions, transactionAdapter)
                    transactionGroupAdapter.addTransactionGroup(group)
                }
                else {
                    for (i in 0 until transactions.size) {
                        // first item
                        if (i == 0) {
                            newTransactions.add(transactions[0])
                            totalAmount += determineTransactionType(
                                transactions[i].transaction_type,
                                transactions[i].transaction_amount
                            )
                        }
                        else {
                            // check if current transaction date is same as previous
                            // increment total amount if same and add to group
                            // otherwise create new group

                            val currentDate =
                                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                                    .format(Date(transactions[i].transaction_date!!))

                            val previousDate =
                                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                                    .format(Date(transactions[i - 1].transaction_date!!))

                            if (currentDate == previousDate) {
                                newTransactions.add(transactions[i])
                                totalAmount += determineTransactionType(
                                    transactions[i].transaction_type,
                                    transactions[i].transaction_amount
                                )

                                // add to adapter if current item is the last item
                                if (i == transactions.size - 1) {
                                    transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                                    group = TransactionGroup(transactions[i].transaction_date, totalAmount, newTransactions, transactionAdapter)
                                    transactionGroupAdapter.addTransactionGroup(group)
                                }
                            }
                            else {
                                // add previous item to current group before deleting data
                                // append items to new list for the ff. reason:
                                // for some odd reason, the list passed to group adapter
                                // still updates when clear() is called even after
                                // adding to group adapter, resulting to loss of previous item's
                                // data

                                val preClear = mutableListOf<Transaction>()
                                for (item in newTransactions) {
                                    preClear.add(item)
                                }

                                transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                                group = TransactionGroup(transactions[i].transaction_date, totalAmount, preClear, transactionAdapter)
                                transactionGroupAdapter.addTransactionGroup(group)

                                // clear current data for the next group
                                totalAmount = 0.0
                                newTransactions.clear()

                                // create new group and add to adapter if current item is last item
                                newTransactions.add(transactions[i])
                                totalAmount += determineTransactionType(
                                    transactions[i].transaction_type,
                                    transactions[i].transaction_amount
                                )

                                // add to adapter if current item is the last item
                                if (i == transactions.size - 1) {
                                    transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                                    group = TransactionGroup(transactions[i].transaction_date, totalAmount, newTransactions, transactionAdapter)
                                    transactionGroupAdapter.addTransactionGroup(group)
                                }
                            }
                        }
                    }
                }

                if (transactionGroupAdapter.itemCount <= 0) {
                    binding.cvTransactionsBreakdownEmpty.visibility = View.VISIBLE
                }

                hideProgressDialog()
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar
                    .make(rootLayout, error.message, 5000)
                    .show()
            }
        }
    }

    private fun determineTransactionType(type: Int, amount: Double): Double {
         return when (type) {
            0 -> 0 - amount
            else -> 0 + amount
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
        binding.cvTransactionsBreakdownEmpty.visibility = View.GONE
        binding.pbTransactionsBreakdown.visibility = View.VISIBLE
        binding.rvTransactionsBreakdown.visibility = View.GONE
        binding.fabAddTransaction.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbTransactionsBreakdown.visibility = View.INVISIBLE
        binding.rvTransactionsBreakdown.visibility = View.VISIBLE
        binding.fabAddTransaction.visibility = View.VISIBLE
    }
}