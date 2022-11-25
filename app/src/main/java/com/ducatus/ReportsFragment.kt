package com.ducatus

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.StrictMode
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Pair
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.ReportExpenseAdapter
import com.ducatus.data.Expense
import com.ducatus.data.Subscription
import com.ducatus.data.Transaction
import com.ducatus.data.ExpenseReport
import com.ducatus.databinding.FragmentReportsBinding
import com.ducatus.interfaces.HomeOverviewInterface
import com.ducatus.pdfservice.FileHandler
import com.ducatus.pdfservice.PdfService
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomsheet.BottomSheetDialog
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
import com.google.gson.Gson
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ReportsFragment : Fragment(), HomeOverviewInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentReportsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var dateRangePicker: MaterialDatePicker<Pair<Long, Long>>
    private lateinit var rootLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private var firebaseUser: FirebaseUser? = null
    private var selectedDate: Long? = null
    private var selectedDateRange: Pair<Long, Long>? = null
    private var selectedDateType: Int = 0
    private var datePickerOption: Int = 0
    private var mutableExpenseReport: MutableList<ExpenseReport>? = null

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value
            }
            if (granted) {
                mutableExpenseReport?.let { createPdf(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        setDatePicker()

        // special case, show date picker when clicked
        binding.rbReportsCalendar.setOnClickListener {
            showPopupDate(it)
        }

        binding.rgReports.setOnCheckedChangeListener { _, checkedId ->
            var zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )
            zdt = zdt.with(LocalTime.MIN)

            if (checkedId != R.id.rbReportsCalendar) {
                var dateText = getString(R.string.last_week)

                when (checkedId) {
                    R.id.rbReportsWeek -> {
                        zdt = zdt.minusDays(7)
                    }
                    R.id.rbReportsMonth -> {
                        zdt = zdt.minusMonths(1)
                        dateText = getString(R.string.last_month)
                    }
                    R.id.rbReportsYear -> {
                        zdt = zdt.minusYears(1)
                        dateText = getString(R.string.last_year)
                    }
                }

                val date = zdt.toInstant().toEpochMilli()
                selectedDate = date
                selectedDateRange = null
                selectedDateType = 0

                binding.tvReportsDate.text = dateText
                firebaseUser?.let { loadTransactions(it.uid, sharedPreferences.accountId.toString(), date, null, 0) }
            }
        }

        binding.ibReportsDownload.setOnClickListener {
            showBottomSheetDialog()
        }
    }

    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem(type: Char, item: String) {
        when (type) {
            'S' -> {
                val intent = Intent(activity, SubscriptionDetailActivity::class.java)
                intent.putExtra("subscriptionId", item)
                startActivity(intent)
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            'T' -> {
                val intent = Intent(activity, TransactionDetailActivity::class.java)
                intent.putExtra("transaction", item)
                startActivity(intent)
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showDeniedDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.reports_permission_denied_title))
            .setMessage(resources.getString(R.string.reports_permission_denied_message))
            .setPositiveButton(resources.getString(R.string.ok)) { _, _ -> }
            .show()
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
        var zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val januaryThisYear = ZonedDateTime.of(zdtToday.year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
        val lastTwentyYears = januaryThisYear.minusYears(20)

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
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            var zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date),
                ZoneId.systemDefault()
            )
            zdt = zdt.with(LocalTime.MIN)
            val startOfDay = zdt.toInstant().toEpochMilli()
            var formattedDate = dtf.format(zdt)

            when (selectedDateType) {
                0 -> {
                    zdtToday = ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneId.systemDefault()
                    )
                    val today = dtf.format(zdtToday)
                    formattedDate = "$formattedDate - $today"

                    selectedDate = startOfDay
                    selectedDateRange = null
                    selectedDateType = 0
                    firebaseUser?.let {
                        loadTransactions(
                            it.uid,
                            sharedPreferences.accountId.toString(),
                            startOfDay,
                            null,
                            0
                        )
                    }
                }
                1 -> {
                    val endOfDay = zdt.with(LocalTime.MAX).toInstant().toEpochMilli()
                    val dateRange = Pair(startOfDay, endOfDay)

                    selectedDate = null
                    selectedDateRange = dateRange
                    selectedDateType = 1
                    firebaseUser?.let {
                        loadTransactions(
                            it.uid,
                            sharedPreferences.accountId.toString(),
                            null,
                            dateRange,
                            1
                        )
                    }
                }
            }

            binding.tvReportsDate.text = formattedDate
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
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val zdtStart = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date.first),
                ZoneId.systemDefault()
            )
            val zdtEnd = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date.second),
                ZoneId.systemDefault()
            )

            val startOfDay = zdtStart.with(LocalTime.MIN).toInstant().toEpochMilli()
            val endOfDay = zdtEnd.with(LocalTime.MAX)

            val formattedStartDate = dtf.format(zdtStart)
            val formattedEndDate = dtf.format(endOfDay)

            val formattedDateRange = "$formattedStartDate - $formattedEndDate"
            binding.tvReportsDate.text = formattedDateRange

            val dateRange = Pair(startOfDay, endOfDay.toInstant().toEpochMilli())

            selectedDate = null
            selectedDateRange = dateRange
            selectedDateType = 1
            firebaseUser?.let { loadTransactions(it.uid, sharedPreferences.accountId.toString(), null, date, 1) }
        }
    }

    private fun loadData() {
        binding.pcReportsChart.setNoDataTextColor(
            ContextCompat.getColor(activity, R.color.slightly_darker_gray)
        )

        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            sharedPreferences = SharedPreferences(activity)
            database = Firebase.database

            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )

            val lastWeek = zdt.with(LocalTime.MIN).minusDays(7).toInstant().toEpochMilli()
            selectedDate = lastWeek
            selectedDateType = 0

            loadTransactions(firebaseUser!!.uid, sharedPreferences.accountId.toString(), lastWeek, null, 0)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadTransactions(uid: String, accountId: String, date: Long?, range: Pair<Long, Long>?, dateType: Int) {
        showProgressDialog()
        databaseReference = database.getReference("transactions").child(uid).child(accountId)

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
                val transactionsReport = mutableListOf<ExpenseReport>()
                val expensesMap = mutableMapOf<String, Double>()
                val expenses = mutableListOf<Expense>()
                val colors = mutableListOf<Int>()
                var totalAmount = 0.0

                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactionsReport.add(
                            ExpenseReport(
                                transaction.name,
                                transaction.amount,
                                transaction.date,
                                transaction.type,
                                transaction.paymentType,
                                transaction.categoryName,
                                transaction.subcategoryName
                            )
                        )

                        // check if transaction is an expense
                        if (transaction.type == 0) {
                            val expense = Expense(
                                null,
                                Gson().toJson(transaction),
                                transaction.name,
                                transaction.amount,
                                transaction.date,
                                'T',
                                transaction.paymentType,
                                transaction.categoryName,
                                transaction.categoryColor,
                                transaction.categoryIcon,
                                transaction.subcategoryName,
                                transaction.subcategoryColor,
                                transaction.subcategoryIcon
                            )

                            expenses.add(expense)

                            val key = transaction.categoryName
                            totalAmount += transaction.amount

                            // check if category is already in map
                            if (!expensesMap.contains(key)) {
                                // set category as key and amount as value
                                expensesMap[key!!] = transaction.amount

                                // add colors to list for new categories
                                val iconColor = resources.getIdentifier(
                                    transaction.categoryColor!!,
                                    "color",
                                    activity.packageName
                                )

                                colors.add(ContextCompat.getColor(activity, iconColor))
                            }
                            else {
                                expensesMap[key!!] = expensesMap[key]!! + transaction.amount
                            }
                        }
                    }
                }

                databaseReference = database.getReference("subscriptions").child(uid).child(accountId)
                loadSubscriptions(date, range, dateType, transactionsReport, expensesMap, expenses, colors, totalAmount)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
                    .show()
            }
    }

    private fun loadSubscriptions(
        date: Long?,
        range: Pair<Long, Long>?,
        dateType: Int,
        transactionsReport: MutableList<ExpenseReport>,
        expensesMap: MutableMap<String, Double>,
        expenses: MutableList<Expense>,
        colors: MutableList<Int>,
        amount: Double
    ) {
        val query =
            when (dateType) {
                0 -> { // default for week/month/year
                    databaseReference.orderByChild("createdAtString").startAt(date.toString())
                }
                1 -> { // date range
                    databaseReference.orderByChild("createdAtString")
                        .startAt(range!!.first.toString())
                        .endAt(range.second.toString())
                }
                else -> databaseReference
            }

        query.get()
            .addOnSuccessListener { snapshot ->
                var totalAmount = amount

                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null) {
                        transactionsReport.add(
                            ExpenseReport(
                                subscription.name,
                                subscription.amount,
                                subscription.createdAt,
                                2,
                                subscription.paymentType,
                                subscription.categoryName,
                                subscription.subcategoryName
                            )
                        )

                        val expense = Expense(
                            subscription.id,
                            null,
                            subscription.name,
                            subscription.amount,
                            subscription.createdAt,
                            'S',
                            subscription.paymentType,
                            subscription.categoryName,
                            subscription.categoryColor,
                            subscription.categoryIcon,
                            subscription.subcategoryName,
                            subscription.subcategoryColor,
                            subscription.subcategoryIcon
                        )

                        expenses.add(expense)

                        val key = subscription.categoryName
                        totalAmount += subscription.amount

                        // check if category is already in map
                        if (!expensesMap.contains(key)) {
                            // set category as key and amount as value
                            expensesMap[key!!] = subscription.amount

                            // add colors to list for new categories
                            val iconColor = resources.getIdentifier(
                                subscription.categoryColor!!,
                                "color",
                                activity.packageName
                            )

                            colors.add(ContextCompat.getColor(activity, iconColor))
                        }
                        else {
                            expensesMap[key!!] = expensesMap[key]!! + subscription.amount
                        }
                    }
                }

                val amountText = "₱" + String.format("%,.2f", totalAmount)
                binding.tvReportsAmount.text = amountText

                val expenseAdapter = ReportExpenseAdapter(mutableListOf(), this)
                binding.rvReports.adapter = expenseAdapter
                binding.rvReports.layoutManager = LinearLayoutManager(activity)

                if (expenses.isNotEmpty()) {
                    binding.ibReportsDownload.visibility = View.VISIBLE
                    binding.tvReportsTopExpenses.text = getString(R.string.top_5_expenses)

                    mutableExpenseReport = transactionsReport
                    generateReport(expensesMap, colors)
                    adaptExpenses(expenses, expenseAdapter)
                }
                else {
                    binding.pcReportsChart.data = null
                    binding.pcReportsChart.invalidate()
                    binding.tvReportsTopExpenses.text = getString(R.string.expenses_empty)
                    hideProgressDialog()
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun generateReport(transactions: Map<String, Double>, colors: List<Int>) {
        val pieChart = binding.pcReportsChart

        // create pie chart entries
        val pieEntries = mutableListOf<PieEntry>()
        for (key in transactions.keys) {
            pieEntries.add(PieEntry(transactions[key]!!.toFloat(), key))
        }

        // pie data set with entries and colors
        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.valueTextSize = 10f
        pieDataSet.sliceSpace = 5f
        pieDataSet.colors = colors

        // pie data
        val pieData = PieData(pieDataSet)
        pieData.setValueTextColor(ContextCompat.getColor(activity, R.color.off_white))
        pieData.setDrawValues(true)
        pieData.setValueFormatter(PercentFormatter(pieChart))

        // set pie chart legend
        val pieLegend = pieChart.legend
        pieLegend.isWordWrapEnabled = true
        pieLegend.form = Legend.LegendForm.CIRCLE
        pieLegend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

        // set pie chart data and properties
        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.setNoDataText(getString(R.string.transactions_empty))
        pieChart.setDrawEntryLabels(false)
        pieChart.description.isEnabled = false
        pieChart.dragDecelerationFrictionCoef = 0.9f
        pieChart.holeRadius = 55f
        pieChart.transparentCircleRadius = 0f
        pieChart.invalidate()
    }

    private fun adaptExpenses(expenses: MutableList<Expense>, expenseAdapter: ReportExpenseAdapter) {
        // sort by highest amount
        expenses.sortByDescending { it.amount }

        // limit to 5 items only
        val size =
            if (expenses.size <= 5) expenses.size
            else 5

        for (i in 0 until size) {
            expenseAdapter.addExpense(expenses[i])
        }

        hideProgressDialog()
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(activity)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_export_reports)

        val exportExpensesReport = bottomSheetDialog.findViewById<TextView>(R.id.tvExportExpensesReport)
        exportExpensesReport?.setOnClickListener {
            mutableExpenseReport?.let {
                if (Build.VERSION.SDK_INT >= 29) {
                    createPdf(it)
                }
                else {
                    if (hasPermissions(activity, PERMISSIONS)) {
                        createPdf(it)
                    }
                    else {
                        requestPermissionsLauncher.launch(PERMISSIONS)
                    }
                }
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun createPdf(transactionsReport: MutableList<ExpenseReport>) {
        val onFinish: (File) -> Unit = { openFile(it) }
        val onError: (Exception) -> Unit = {
            Snackbar
                .make(rootLayout, "PDF Error", 5000)
                .show()
        }

        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val formattedDateToday = dtf.format(zdtToday)
        val formattedDate = when (selectedDateType) {
            0 -> { // specific date
                val zdtSelectedDate = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(selectedDate!!),
                    ZoneId.systemDefault()
                )
                val formatted = dtf.format(zdtSelectedDate)
                "$formatted - $formattedDateToday"
            }
            else -> { // start date / date range
                val zdtStart = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(selectedDateRange!!.first),
                    ZoneId.systemDefault()
                )
                val zdtEnd = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(selectedDateRange!!.second),
                    ZoneId.systemDefault()
                )
                val formattedStartDate = dtf.format(zdtStart)
                val formattedEndDate = dtf.format(zdtEnd)
                "$formattedStartDate - $formattedEndDate"
            }
        }

        val pdfService = PdfService()
        pdfService.createPdf(
            sharedPreferences.accountName.toString(),
            formattedDate,
            transactionsReport,
            onFinish,
            onError
        )
    }

    private fun openFile(file: File) {
        val path = FileHandler().getRealPath(activity, file.toUri())
        val pdfFile = path?.let { File(it) }
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        val pdfIntent = Intent(Intent.ACTION_VIEW)
        if (pdfFile != null) {
            pdfIntent.setDataAndType(pdfFile.toUri(), "application/pdf")
        }
        pdfIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        try {
            startActivity(pdfIntent)
        }
        catch (exception: ActivityNotFoundException) {
            Snackbar
                .make(rootLayout, exception.localizedMessage!!, 5000)
                .show()
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

    private fun showProgressDialog() {
        binding.pbReports.visibility = View.VISIBLE
        binding.ibReportsDownload.visibility = View.GONE
        binding.rvReports.visibility = View.INVISIBLE

        val amountText = "₱0.00"
        binding.tvReportsAmount.text = amountText
    }

    private fun hideProgressDialog() {
        binding.pbReports.visibility = View.INVISIBLE
        binding.rvReports.visibility = View.VISIBLE
    }
}