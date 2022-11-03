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
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.data.Transaction
import com.ducatus.data.TransactionGroup
import com.ducatus.databinding.FragmentTransactionsBreakdownBinding
import com.ducatus.viewmodel.SearchViewModel
import com.google.android.material.appbar.MaterialToolbar
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
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private var firebaseUser: FirebaseUser? = null
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)
        binding = FragmentTransactionsBreakdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        setDatePicker()

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    firebaseUser?.let {
                        val fragmentManager = childFragmentManager
                        val newFragment = SearchItemDialogFragment()
                        newFragment.show(fragmentManager, "dialog")
                    }
                    true
                }
                else -> false
            }
        }

        searchViewModel.searchInput.observe(viewLifecycleOwner) { name ->
            searchTransactionsByCategory(name)
        }

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

                firebaseUser?.let { loadTransactions(date) }
            }
        }

        binding.fabAddTransaction.setOnClickListener {
            val intent = Intent(activity, TransactionAddActivity::class.java)
            startActivity(intent)
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            toolbar.menu.clear()
        }
    }

    override fun onResume() {
        super.onResume()

        toolbar.inflateMenu(R.menu.search_menu)
        firebaseUser?.let {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val lastWeek = calendar.timeInMillis
            loadTransactions(lastWeek)
        }
    }

    override fun onPause() {
        super.onPause()
        toolbar.menu.clear()
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem(categoryId: String, transactionId: String) {
        val intent = Intent(activity, TransactionDetailActivity::class.java)
        intent.putExtra("categoryId", categoryId)
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
            loadTransactions(date)
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference = database.getReference("transactions").child(firebaseUser!!.uid).child(currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadTransactions(date: Long) {
        showProgressDialog()
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val transactions = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        if (transaction.date!! >= date) {
                            transactions.add(transaction)
                        }
                    }
                }

                // sort by date
                transactions.sortByDescending {
                    it.date!! + it.hour!! + it.minute!!
                }

                adaptTransactions(transactions)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun searchTransactionsByCategory(name: String) {
        showProgressDialog()
        val query = databaseReference.orderByChild("categoryNameLower").startAt(name).endAt(name + "\uf8ff")
        query.get()
            .addOnSuccessListener { snapshot ->
                val transactions = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }

                // search by subcategory and add to list
                searchTransactionsBySubcategory(name, transactions)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun searchTransactionsBySubcategory(name: String, transactions: MutableList<Transaction>) {
        val query = databaseReference.orderByChild("subcategoryNameLower").startAt(name).endAt(name + "\uf8ff")
        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }

                if (transactions.isNotEmpty()) {
                    // sort by date
                    transactions.sortByDescending {
                        it.date!! + it.hour!! + it.minute!!
                    }

                    adaptTransactions(transactions)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "No transactions found with the name $name", Snackbar.LENGTH_LONG)
                        .show()
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun adaptTransactions(transactions: MutableList<Transaction>) {
        val transactionGroupAdapter = TransactionGroupAdapter(mutableListOf(), this)
        binding.rvTransactionsBreakdown.adapter = transactionGroupAdapter
        binding.rvTransactionsBreakdown.layoutManager = LinearLayoutManager(activity)

        var transactionAdapter: TransactionAdapter
        var group: TransactionGroup

        val newTransactions = mutableListOf<Transaction>()
        var totalAmount = 0.0

        if (transactions.size == 1) {
            newTransactions.add(transactions[0])
            totalAmount += determineTransactionType(
                transactions[0].type,
                transactions[0].amount
            )

            transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
            group = TransactionGroup(transactions[0].date, totalAmount, transactions, transactionAdapter)
            transactionGroupAdapter.addTransactionGroup(group)
        }
        else {
            for (i in 0 until transactions.size) {
                // first item
                if (i == 0) {
                    newTransactions.add(transactions[0])
                    totalAmount += determineTransactionType(
                        transactions[i].type,
                        transactions[i].amount
                    )
                }
                else {
                    // check if current transaction date is same as previous
                    // increment total amount if same and add to group
                    // otherwise create new group

                    val currentDate =
                        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                            .format(Date(transactions[i].date!!))

                    val previousDate =
                        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                            .format(Date(transactions[i - 1].date!!))

                    if (currentDate == previousDate) {
                        newTransactions.add(transactions[i])
                        totalAmount += determineTransactionType(
                            transactions[i].type,
                            transactions[i].amount
                        )

                        // add to adapter if current item is the last item
                        if (i == transactions.size - 1) {
                            transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                            group = TransactionGroup(transactions[i].date, totalAmount, newTransactions, transactionAdapter)
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
                        group = TransactionGroup(transactions[i].date, totalAmount, preClear, transactionAdapter)
                        transactionGroupAdapter.addTransactionGroup(group)

                        // clear current data for the next group
                        totalAmount = 0.0
                        newTransactions.clear()

                        // create new group and add to adapter if current item is last item
                        newTransactions.add(transactions[i])
                        totalAmount += determineTransactionType(
                            transactions[i].type,
                            transactions[i].amount
                        )

                        // add to adapter if current item is the last item
                        if (i == transactions.size - 1) {
                            transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                            group = TransactionGroup(transactions[i].date, totalAmount, newTransactions, transactionAdapter)
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