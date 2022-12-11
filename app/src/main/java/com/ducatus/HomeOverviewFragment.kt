package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.HomeTransactionAdapter
import com.ducatus.data.Account
import com.ducatus.data.Subscription
import com.ducatus.data.Transaction
import com.ducatus.databinding.FragmentHomeOverviewBinding
import com.ducatus.interfaces.HomeOverviewInterface
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeOverviewFragment : Fragment(), HomeOverviewInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentHomeOverviewBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var navigationView: NavigationView
    private lateinit var rootLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        navigationView = activity.findViewById(R.id.nvHome)
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentHomeOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.tvHomeViewReports.setOnClickListener {
            toolbar.setTitle(R.string.reports)
            val reportItem = navigationView.menu.findItem(R.id.nav_reports)
            reportItem.isChecked = true

            val action = HomeFragmentDirections.actionHomeFragmentToReportsFragment()
            findNavController().navigate(action)
        }

        binding.tvHomeViewTransactions.setOnClickListener {
            toolbar.setTitle(R.string.transactions)
            val transactionItem = navigationView.menu.findItem(R.id.nav_transactions)
            transactionItem.isChecked = true

            val action = HomeFragmentDirections.actionHomeFragmentToTransactionsFragment()
            findNavController().navigate(action)
        }
    }

    override fun viewItem(type: Char, item: String) {
        when (type) {
            'T' -> {
                val intent = Intent(activity, TransactionDetailActivity::class.java)
                intent.putExtra("transaction", item)
                startActivity(intent)
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun loadData() {
        binding.pcHomeExpensesChart.setNoDataTextColor(
            ContextCompat.getColor(activity, R.color.slightly_darker_gray)
        )

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            loadAccount(firebaseUser.uid)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccount(uid: String) {
        databaseReference = database.getReference("accounts").child(uid)
        sharedPreferences = SharedPreferences(activity)
        val currentAccountId = sharedPreferences.accountId.toString()

        if (currentAccountId.isNotEmpty()) {
            databaseReference.child(currentAccountId).get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        getFirstAccount(uid)
                    }
                    else {
                        val account = snapshot.getValue<Account>()
                        account?.let {
                            loadAccountData(it)
                            loadTransactions(uid, currentAccountId)
                            loadRecentTransactions(uid, currentAccountId)
                        }
                    }
                }
                .addOnFailureListener {
                    Snackbar
                        .make(rootLayout, getString(R.string.load_account_error), 5000)
                        .show()
                }
        }
        else {
            getFirstAccount(uid)
        }
    }

    private fun getFirstAccount(uid: String) {
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val account = child.getValue<Account>()
                    account?.let {
                        sharedPreferences.accountId = it.id
                        sharedPreferences.accountName = it.name
                        sharedPreferences.accountColor = it.color
                        val accountId = sharedPreferences.accountId!!

                        loadAccountData(it)
                        loadTransactions(uid, accountId)
                        loadRecentTransactions(uid, accountId)
                    }

                    break
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_account_error), 5000)
                    .show()
            }
    }

    private fun loadAccountData(account: Account) {
        val iconColor = activity.resources.getIdentifier(
            account.color,
            "color",
            activity.packageName
        )

        binding.ivHomeAccountIcon.setColorFilter(
            ResourcesCompat.getColor(
                activity.resources,
                iconColor,
                null
            )
        )

        val name =
            account.name?.let {
                if (it.contains(" "))  it.split(" ")[0]
                else it
            }

        binding.tvHomeAccountName.text = name

        val budget = "₱" + String.format("%,.2f", account.remainingBalance)
        binding.tvHomeAccountBalance.text = budget
    }

    private fun loadTransactions(uid: String, accountId: String) {
        showProgressDialogExpensesReport()
        databaseReference = database.getReference("transactions").child(uid).child(accountId)

        val zdt = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val lastWeek = zdt.with(LocalTime.MIN).minusDays(7).toInstant().toEpochMilli()

        val query = databaseReference.orderByChild("dateString").startAt(lastWeek.toString())
        query.get()
            .addOnSuccessListener { snapshot ->
                val expenses = mutableMapOf<String, Double>()
                val colors = mutableListOf<Int>()
                var totalAmount = 0.0

                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        // check if transaction is an expense
                        if (transaction.type == 0) {
                            val key = transaction.categoryName
                            totalAmount += transaction.amount

                            // check if category is already in map
                            if (!expenses.contains(key)) {
                                // set category as key and amount as value
                                expenses[key!!] = transaction.amount

                                // add colors to list for new categories
                                val iconColor = activity.resources.getIdentifier(
                                    transaction.categoryColor!!,
                                    "color",
                                    activity.packageName
                                )

                                colors.add(ContextCompat.getColor(activity, iconColor))
                            }
                            else {
                                expenses[key!!] = expenses[key]!! + transaction.amount
                            }
                        }
                    }
                }

                databaseReference = database.getReference("subscriptions").child(uid).child(accountId)
                loadSubscriptions(lastWeek, expenses, colors, totalAmount)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
                    .show()
            }
    }

    private fun loadSubscriptions(
        date: Long?,
        expenses: MutableMap<String, Double>,
        colors: MutableList<Int>,
        amount: Double
    ) {
        val query = databaseReference.orderByChild("createdAtString").startAt(date.toString())
        query.get()
            .addOnSuccessListener { snapshot ->
                var totalAmount = amount

                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null) {
                        val key = subscription.categoryName
                        totalAmount += subscription.amount

                        // check if category is already in map
                        if (!expenses.contains(key)) {
                            // set category as key and amount as value
                            expenses[key!!] = subscription.amount

                            // add colors to list for new categories
                            val iconColor = activity.resources.getIdentifier(
                                subscription.categoryColor!!,
                                "color",
                                activity.packageName
                            )

                            colors.add(ContextCompat.getColor(activity, iconColor))
                        }
                        else {
                            expenses[key!!] = expenses[key]!! + subscription.amount
                        }
                    }
                }

                val amountText = "₱" + String.format("%,.2f", totalAmount)
                binding.tvHomeExpensesReportAmount.text = amountText

                if (expenses.isNotEmpty()) {
                    generateExpensesReport(expenses, colors)
                }
                else {
                    hideProgressDialogExpensesReport()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun generateExpensesReport(expenses: Map<String, Double>, colors: List<Int>) {
        val pieChart = binding.pcHomeExpensesChart

        // create pie chart entries
        val pieEntries = mutableListOf<PieEntry>()
        for (key in expenses.keys) {
            pieEntries.add(PieEntry(expenses[key]!!.toFloat(), key))
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
        pieChart.setNoDataText(activity.getString(R.string.transactions_empty))
        pieChart.setDrawEntryLabels(false)
        pieChart.description.isEnabled = false
        pieChart.dragDecelerationFrictionCoef = 0.9f
        pieChart.holeRadius = 55f
        pieChart.transparentCircleRadius = 0f
        pieChart.invalidate()

        hideProgressDialogExpensesReport()
    }

    private fun loadRecentTransactions(uid: String, accountId: String) {
        showProgressDialogRecentTransactions()
        databaseReference = database.getReference("transactions").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val homeTransactionAdapter = HomeTransactionAdapter(mutableListOf(), this@HomeOverviewFragment)
                binding.rvHomeRecentTransactions.adapter = homeTransactionAdapter
                binding.rvHomeRecentTransactions.layoutManager = LinearLayoutManager(activity)

                val transactions = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }

                // sort by latest update
                transactions.sortByDescending { it.date!! }

                // limit to 3 items only
                val size =
                    if (transactions.size <= 3) transactions.size
                    else 3

                for (i in 0 until size) {
                    homeTransactionAdapter.addTransaction(transactions[i])
                }

                if (homeTransactionAdapter.itemCount <= 0) {
                    binding.tvHomeRecentTransactionsEmpty.visibility = View.VISIBLE
                }

                hideProgressDialogRecentTransactions()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_transactions_error), 5000)
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

    private fun showProgressDialogExpensesReport() {
        binding.pbHomeExpensesReport.visibility = View.VISIBLE
    }

    private fun hideProgressDialogExpensesReport() {
        binding.pbHomeExpensesReport.visibility = View.INVISIBLE
    }

    private fun showProgressDialogRecentTransactions() {
        binding.pbHomeRecentTransactions.visibility = View.VISIBLE
        binding.rvHomeRecentTransactions.visibility = View.GONE
    }

    private fun hideProgressDialogRecentTransactions() {
        binding.pbHomeRecentTransactions.visibility = View.INVISIBLE
        binding.rvHomeRecentTransactions.visibility = View.VISIBLE
    }
}