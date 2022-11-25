package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.util.Pair
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.TransactionAdapter
import com.ducatus.adapter.TransactionGroupAdapter
import com.ducatus.data.Transaction
import com.ducatus.data.TransactionGroup
import com.ducatus.databinding.FragmentTransactionsBreakdownBinding
import com.ducatus.interfaces.TransactionInterface
import com.ducatus.viewmodel.SearchViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TransactionsBreakdownFragment : Fragment(), TransactionInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentTransactionsBreakdownBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var dateRangePicker: MaterialDatePicker<Pair<Long, Long>>
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private var firebaseUser: FirebaseUser? = null
    private var selectedDate: Long? = null
    private var selectedDateRange: Pair<Long, Long>? = null
    private var selectedDateType: Int = 0
    private var datePickerOption: Int = 0
    private var mutableTransactions: MutableList<Transaction>? = null
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
            name.getContentIfNotHandled()?.let { content ->
                searchTransactionsByName(content.lowercase())
            }
        }

        // special case, show date picker when clicked
        binding.rbTransactionsBreakdownCalendar.setOnClickListener {
            showPopupDate(it)
        }

        binding.rgTransactionsBreakdown.setOnCheckedChangeListener { _, checkedId ->
            var zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )

            zdt = zdt.with(LocalTime.MIN)

            if (checkedId != R.id.rbTransactionsBreakdownCalendar) {
                when (checkedId) {
                    R.id.rbTransactionsBreakdownWeek -> {
                        zdt = zdt.minusDays(7)
                    }
                    R.id.rbTransactionsBreakdownMonth -> {
                        zdt = zdt.minusMonths(1)
                    }
                    R.id.rbTransactionsBreakdownYear -> {
                        zdt = zdt.minusYears(1)
                    }
                }

                val date = zdt.toInstant().toEpochMilli()
                selectedDate = date
                selectedDateRange = null
                selectedDateType = 0

                firebaseUser?.let { loadTransactions(date, null, 0) }
            }
        }

        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(activity, TransactionAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onResume() {
        super.onResume()

        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.search_menu)
        firebaseUser?.let { loadTransactions(selectedDate, selectedDateRange, selectedDateType) }
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem(transaction: Transaction) {
        val intent = Intent(activity, TransactionDetailActivity::class.java)
        intent.putExtra("transaction", Gson().toJson(transaction))
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showPopupDate(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.date -> { // start of day to end of day
                    if (!datePicker.isAdded) {
                        datePicker.show(childFragmentManager, "tag")
                        datePickerOption = 1
                    }
                    true
                }
                R.id.dateStart -> { // start date until now
                    if (!datePicker.isAdded) {
                        datePicker.show(childFragmentManager, "tag")
                        datePickerOption = 0
                    }
                    true
                }
                R.id.dateRange -> { // start date to end date
                    if (!dateRangePicker.isAdded) {
                        dateRangePicker.show(childFragmentManager, "tag")
                    }
                    true
                }
                else -> false
            }
        }

        // menu to inflate
        popup.menuInflater.inflate(R.menu.date_options_menu, popup.menu)
        popup.show()
    }

    private fun setDatePicker() {
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val janThisYear = ZonedDateTime.of(zdtToday.year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
        val lastTwentyYears = janThisYear.minusYears(20)

        val startDate = lastTwentyYears.toInstant().toEpochMilli()
        val endDate = zdtToday.toInstant().toEpochMilli()

        // Build constraints.
        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .setStart(startDate)
                .setEnd(endDate)

        datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

        datePicker.addOnPositiveButtonClickListener { date ->
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date),
                ZoneId.systemDefault()
            )
            val startOfDay = zdt.with(LocalTime.MIN).toInstant().toEpochMilli()

            when (datePickerOption) {
                0 -> {
                    selectedDate = startOfDay
                    selectedDateRange = null
                    selectedDateType = 0
                    firebaseUser?.let { loadTransactions(startOfDay, null, 0) }
                }
                1 -> {
                    val endOfDay = zdt.with(LocalTime.MAX).toInstant().toEpochMilli()
                    val dateRange = Pair(startOfDay, endOfDay)

                    selectedDate = null
                    selectedDateRange = dateRange
                    selectedDateType = 1
                    firebaseUser?.let { loadTransactions(null, dateRange, 1) }
                }
            }
        }

        dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select dates")
                .setSelection(
                    Pair(
                        MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

        dateRangePicker.addOnPositiveButtonClickListener { date ->
            val zdtStart = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date.first),
                ZoneId.systemDefault()
            )
            val zdtEnd = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date.second),
                ZoneId.systemDefault()
            )

            val startOfDay = zdtStart.with(LocalTime.MIN).toInstant().toEpochMilli()
            val endOfDay = zdtEnd.with(LocalTime.MAX).toInstant().toEpochMilli()
            val dateRange = Pair(startOfDay, endOfDay)

            selectedDate = null
            selectedDateRange = dateRange
            selectedDateType = 1
            firebaseUser?.let { loadTransactions(null, dateRange, 1) }
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )

            val lastWeek = zdt.with(LocalTime.MIN).minusDays(7).toInstant().toEpochMilli()
            selectedDate = lastWeek
            selectedDateRange = null
            selectedDateType = 0

            val sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference =
                database.getReference("transactions")
                    .child(firebaseUser!!.uid)
                    .child(currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadTransactions(date: Long?, range: Pair<Long, Long>?, dateType: Int) {
        showProgressDialog()
        val query =
            when (dateType) {
                0 -> { // start date / default for week/month/year
                    databaseReference.orderByChild("dateString").startAt(date.toString())
                }
                1 -> { // date range
                    databaseReference.orderByChild("dateString")
                        .startAt(range!!.first.toString())
                        .endAt(range.second.toString())
                }
                else -> databaseReference
            }

        query.get()
            .addOnSuccessListener { snapshot ->
                val transactions = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }

                // sort by newest date
                transactions.sortByDescending { it.date!! }
                adaptTransactions(transactions)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
                    .show()
            }
    }

    private fun searchTransactionsByName(name: String) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("nameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                val transactions = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }

                // search by category and add to list
                searchTransactionsByCategory(name, transactions)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
                    .show()
            }
    }

    private fun searchTransactionsByCategory(name: String, transactions: MutableList<Transaction>) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("categoryNameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null && !transactions.contains(transaction)) {
                        transactions.add(transaction)
                    }
                }

                // search by subcategory and add to list
                searchTransactionsBySubcategory(name, transactions)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
                    .show()
            }
    }

    private fun searchTransactionsBySubcategory(name: String, transactions: MutableList<Transaction>) {
        val query =
            databaseReference
                .orderByChild("subcategoryNameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null && !transactions.contains(transaction)) {
                        transactions.add(transaction)
                    }
                }

                if (transactions.isNotEmpty()) {
                    // sort by newest date
                    transactions.sortByDescending { it.date!! }
                    adaptTransactions(transactions)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "No transactions found with the name $name", Snackbar.LENGTH_LONG)
                        .show()

                    if (mutableTransactions?.isNotEmpty() == true) {
                        binding.tvTransactionsBreakdownSort.visibility = View.VISIBLE
                    }
                    else {
                        binding.tvTransactionsBreakdownSort.visibility = View.GONE
                        binding.cvTransactionsBreakdownEmpty.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
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

                    val currentDate = transactions[i].date!!
                    val previousDate = transactions[i - 1].date!!

                    val zdtCurrent = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(currentDate),
                        ZoneId.systemDefault()
                    )

                    val zdtPrevious = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(previousDate),
                        ZoneId.systemDefault()
                    )

                    val epochCurrent = zdtCurrent.with(LocalTime.MIN).toInstant().toEpochMilli()
                    val epochPrevious = zdtPrevious.with(LocalTime.MIN).toInstant().toEpochMilli()

                    if (epochCurrent == epochPrevious) {
                        newTransactions.add(transactions[i])
                        totalAmount += determineTransactionType(
                            transactions[i].type,
                            transactions[i].amount
                        )

                        // add to adapter if current item is the last item
                        if (i == transactions.size - 1) {
                            transactionAdapter = TransactionAdapter(mutableListOf(), this@TransactionsBreakdownFragment)
                            group = TransactionGroup(currentDate, totalAmount, newTransactions, transactionAdapter)
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
                        group = TransactionGroup(previousDate, totalAmount, preClear, transactionAdapter)
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
                            group = TransactionGroup(currentDate, totalAmount, newTransactions, transactionAdapter)
                            transactionGroupAdapter.addTransactionGroup(group)
                        }
                    }
                }
            }
        }

        if (transactionGroupAdapter.itemCount <= 0) {
            mutableTransactions = null
            binding.tvTransactionsBreakdownSort.visibility = View.GONE
            binding.cvTransactionsBreakdownEmpty.visibility = View.VISIBLE
        }
        else {
            mutableTransactions = transactions
            binding.tvTransactionsBreakdownSort.visibility = View.VISIBLE
            binding.tvTransactionsBreakdownSort.setOnClickListener { showPopup(it) }
        }

        hideProgressDialog()
    }

    private fun determineTransactionType(type: Int, amount: Double): Double {
        return when (type) {
            0 -> 0 - amount
            else -> 0 + amount
        }
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.amountLowest -> {
                    mutableTransactions?.let { transactions ->
                        transactions.sortBy { it.amount }
                        adaptTransactions(transactions)
                    }

                    true
                }
                R.id.amountHighest -> {
                    mutableTransactions?.let { transactions ->
                        transactions.sortByDescending { it.amount }
                        adaptTransactions(transactions)
                    }

                    true
                }
                R.id.sortDateAddedOldest -> {
                    mutableTransactions?.let { transactions ->
                        transactions.sortBy { it.date!! }
                        adaptTransactions(transactions)
                    }

                    true
                }
                R.id.sortDateAddedNewest -> {
                    mutableTransactions?.let { transactions ->
                        transactions.sortByDescending { it.date!! }
                        adaptTransactions(transactions)
                    }

                    true
                }
                else -> false
            }
        }

        // menu to inflate
        popup.menuInflater.inflate(R.menu.sort_options_menu, popup.menu)
        popup.show()
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

    private fun showProgressDialog() {
        binding.cvTransactionsBreakdownEmpty.visibility = View.GONE
        binding.tvTransactionsBreakdownSort.visibility = View.GONE
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