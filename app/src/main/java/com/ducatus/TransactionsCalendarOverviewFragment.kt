package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.util.Pair
import androidx.drawerlayout.widget.DrawerLayout
import com.ducatus.data.Transaction
import com.ducatus.databinding.FragmentTransactionsCalendarOverviewBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
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
import java.time.*
import java.time.format.DateTimeFormatter

class TransactionsCalendarOverviewFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentTransactionsCalendarOverviewBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private var firebaseUser: FirebaseUser? = null
    private var selectedDateType: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)
        toolbar.menu.clear()

        binding = FragmentTransactionsCalendarOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        setCalendar()
    }

    private fun setCalendar() {
        var zdt = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val todayStartOfDay = zdt.with(LocalTime.MIN)
        val todayEpoch = todayStartOfDay.toInstant().toEpochMilli()

        firebaseUser?.let { loadTransactions(todayEpoch, null, 0) }

        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        var formattedDate = dtf.format(todayStartOfDay)
        binding.tvTransactionsCalendarDate.text = formattedDate

        val januaryThisYear = ZonedDateTime.of(zdt.year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
        val lastTwentyYears = januaryThisYear.minusYears(20)
        val startDate = lastTwentyYears.toInstant().toEpochMilli()

        setDatePicker(startDate, todayEpoch)
        binding.cvTransactionsCalendar.minDate = startDate
        binding.cvTransactionsCalendar.maxDate = todayEpoch

        // calendar change should be from start of day to end of day
        binding.cvTransactionsCalendar.setOnDateChangeListener { _, year, month, day ->
            zdt = ZonedDateTime.of(year, month + 1, day, 0, 0, 0, 0, ZoneId.systemDefault())
            formattedDate = dtf.format(zdt)
            binding.tvTransactionsCalendarDate.text = formattedDate

            val startOfDay = zdt.with(LocalTime.MIN).toInstant().toEpochMilli()
            val endOfDay = zdt.with(LocalTime.MAX).toInstant().toEpochMilli()
            val dateRange = Pair(startOfDay, endOfDay)
            loadTransactions(null, dateRange, 1)
        }
    }

    private fun setDatePicker(startDate: Long, endDate: Long) {
        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .setStart(startDate)
                .setEnd(endDate)

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

        datePicker.addOnPositiveButtonClickListener { date ->
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            var zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date),
                ZoneId.systemDefault()
            )
            zdt = zdt.with(LocalTime.MIN)
            val startOfDay = zdt.toInstant().toEpochMilli()
            var formattedDate = dtf.format(zdt)

            when (selectedDateType) {
                0 -> { // start date until now
                    val zdtToday = ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneId.systemDefault()
                    )
                    val today = dtf.format(zdtToday)
                    formattedDate = "$formattedDate - $today"
                    loadTransactions(startOfDay, null, 0)
                }
                1 -> { // start of day to end of day
                    val endOfDay = zdt.with(LocalTime.MAX).toInstant().toEpochMilli()
                    val dateRange = Pair(startOfDay, endOfDay)
                    loadTransactions(null, dateRange, 1)
                }
            }

            binding.cvTransactionsCalendar.date = startOfDay
            binding.tvTransactionsCalendarDate.text = formattedDate
        }

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
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
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val zdtStart = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date.first),
                ZoneId.systemDefault()
            )
            val zdtEnd = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date.second),
                ZoneId.systemDefault()
            )

            val startOfDay = zdtStart.with(LocalTime.MIN)
            val endOfDay = zdtEnd.with(LocalTime.MAX)

            val formattedStartDate = dtf.format(zdtStart)
            val formattedEndDate = dtf.format(endOfDay)

            val formattedDateRange = "$formattedStartDate - $formattedEndDate"
            binding.tvTransactionsCalendarDate.text = formattedDateRange
            binding.cvTransactionsCalendar.date = date.first

            val dateRange = Pair(
                startOfDay.toInstant().toEpochMilli(),
                endOfDay.toInstant().toEpochMilli()
            )

            loadTransactions(null, dateRange, 1)
        }

        binding.tvTransactionsCalendarHelper.setOnClickListener {
            showPopup(it, datePicker, dateRangePicker)
        }
    }

    private fun showPopup(view: View, datePicker: MaterialDatePicker<Long>, dateRangePicker: MaterialDatePicker<Pair<Long, Long>>) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.date -> { // start of day to end of day
                    if (!datePicker.isAdded) {
                        datePicker.show(childFragmentManager, "tag")
                        selectedDateType = 1
                    }
                    true
                }
                R.id.dateStart -> { // start date until now
                    if (!datePicker.isAdded) {
                        datePicker.show(childFragmentManager, "tag")
                        selectedDateType = 0
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

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
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
                0 -> { // start date
                    databaseReference.orderByChild("dateString").startAt(date.toString())
                }
                1 -> { // date range
                    databaseReference.orderByChild("dateString")
                        .startAt(range!!.first.toString())
                        .endAt(range.second.toString())
                }
                else -> databaseReference.orderByChild("dateString").equalTo(date.toString())
            }

        query.get()
            .addOnSuccessListener { snapshot ->
                val transactions = mutableListOf<Transaction>()
                val transactionType = mutableListOf(0, 0) // expense and income count

                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactions.add(transaction)
                        when (transaction.type) {
                            0 -> transactionType[0]++
                            1 -> transactionType[1]++
                        }
                    }
                }

                val frequencies = transactions.groupingBy { it.categoryName }.eachCount()
                var category = ""
                for (frequency in frequencies) {
                    category = frequency.key.toString()
                    break
                }

                val totalTransactionCount = transactionType[0] + transactionType[1]
                if (totalTransactionCount > 0) {
                    showInfo(
                        mapOf(
                            "category" to category,
                            "count" to totalTransactionCount.toString(),
                            "expenses" to transactionType[0].toString(),
                            "income" to transactionType[1].toString()
                        )
                    )
                }
                else {
                    hideInfo()
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
                    .show()
            }
    }

    private fun showInfo(info: Map<String, String>) {
        binding.tvTransactionCalendarCategory.text = info["category"]
        binding.tvTransactionsCalendarCount.text = info["count"]
        binding.tvTransactionsCalendarExpenses.text = info["expenses"]
        binding.tvTransactionsCalendarIncome.text = info["income"]

        binding.tvTransactionCalendarMost.visibility = View.VISIBLE
        binding.tvTransactionCalendarCategory.visibility = View.VISIBLE
        binding.llTransactionCalendarCount.visibility = View.VISIBLE
        binding.llTransactionCalendarExpenses.visibility = View.VISIBLE
        binding.llTransactionCalendarIncome.visibility = View.VISIBLE
    }

    private fun hideInfo() {
        binding.tvTransactionsCalendarEmpty.visibility = View.VISIBLE
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
        binding.pbTransactionsCalendar.visibility = View.VISIBLE
        binding.tvTransactionsCalendarEmpty.visibility = View.GONE
        binding.tvTransactionCalendarMost.visibility = View.GONE
        binding.tvTransactionCalendarCategory.visibility = View.GONE
        binding.llTransactionCalendarCount.visibility = View.GONE
        binding.llTransactionCalendarExpenses.visibility = View.GONE
        binding.llTransactionCalendarIncome.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbTransactionsCalendar.visibility = View.INVISIBLE
    }
}