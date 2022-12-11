package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.ducatus.data.Account
import com.ducatus.data.Budget
import com.ducatus.databinding.ActivityHomeBinding
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

        binding.tbHome.setNavigationOnClickListener {
            binding.dlHome.open()
        }

        binding.nvHome.setNavigationItemSelectedListener { menuItem ->
            binding.tbHome.menu.clear()
            var title = R.string.home

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.homeFragment)
                }
                R.id.nav_reports -> {
                    title = R.string.reports
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.reportsFragment)
                }
                R.id.nav_budgets -> {
                    title = R.string.budgets
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.budgetsFragment)
                }
                R.id.nav_transactions -> {
                    title = R.string.transactions
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.transactionsFragment)
                }
                R.id.nav_subscriptions -> {
                    title = R.string.subscriptions
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.subscriptionsViewPagerFragment)
                }
                R.id.nav_loans -> {
                    title = R.string.loans
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.loansFragment)
                }
                R.id.nav_goals -> {
                    title = R.string.goals
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.goalsFragment)
                }
                R.id.nav_challenges -> {
                    title = R.string.challenges
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.challengesFragment)
                }
                R.id.nav_tips -> {
                    title = R.string.tips
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.tipsFragment)
                }
//                R.id.nav_help -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, HelpFragment()).commit()
//                }
                R.id.nav_settings -> {
                    title = R.string.settings
                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.settingsFragment)
                }
            }

            menuItem.isChecked = true
            binding.tbHome.setTitle(title)
            binding.dlHome.close()
            true
        }
    }

    override fun onBackPressed() {
        if (binding.dlHome.isOpen) {
            binding.dlHome.close()
        }
        else {
            binding.dlHome.open()
        }
    }

    override fun onResume() {
        super.onResume()

        firebaseUser?.let { loadAccount(it.uid) }
        hasNotification()
    }

    /**
     * Check notification type upon opening app from notification
     */
    private fun hasNotification() {
        val notificationIntent = intent.getStringExtra("notification")
        if (notificationIntent != null) {
            val accountId = intent.getStringExtra("accountId")
            val itemId = intent.getStringExtra("itemId")

            when (notificationIntent) {
                "challenge" -> {
                    intent.removeExtra("notification")
                    intent.removeExtra("accountId")
                    intent.removeExtra("itemId")

                    binding.tbHome.menu.clear()
                    binding.tbHome.setTitle(R.string.challenges)
                    binding.nvHome.menu.findItem(R.id.nav_challenges).isChecked = true

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.challengesFragment)
                }
                "expense" -> {
                    intent.removeExtra("notification")
                    intent.removeExtra("accountId")
                    intent.removeExtra("itemId")

                    binding.tbHome.menu.clear()
                    binding.tbHome.setTitle(R.string.transactions)
                    binding.nvHome.menu.findItem(R.id.nav_transactions).isChecked = true

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.transactionsFragment)
                }

                "subscription" -> {
                    intent.removeExtra("notification")
                    intent.removeExtra("accountId")
                    intent.removeExtra("itemId")

                    binding.tbHome.menu.clear()
                    binding.tbHome.setTitle(R.string.subscriptions)
                    binding.nvHome.menu.findItem(R.id.nav_subscriptions).isChecked = true

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.subscriptionsViewPagerFragment)

                    val subscriptionIntent = Intent(this, SubscriptionDetailActivity::class.java)
                    subscriptionIntent.putExtra("accountId", accountId)
                    subscriptionIntent.putExtra("subscriptionId", itemId)
                    startActivity(subscriptionIntent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_in_left)
                }
                "loan" -> {
                    intent.removeExtra("notification")
                    intent.removeExtra("accountId")
                    intent.removeExtra("itemId")

                    binding.tbHome.menu.clear()
                    binding.tbHome.setTitle(R.string.loans)
                    binding.nvHome.menu.findItem(R.id.nav_loans).isChecked = true

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigate(R.id.loansFragment)

                    val loanIntent = Intent(this, LoanDetailActivity::class.java)
                    loanIntent.putExtra("accountId", accountId)
                    loanIntent.putExtra("loanId", itemId)
                    startActivity(loanIntent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_in_left)
                }
            }
        }
    }

    /**
     * Load dependencies
     */
    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        firebaseUser?.run {
            sharedPreferences = SharedPreferences(applicationContext)
            database = Firebase.database
            databaseReference = database.getReference("accounts").child(uid)
            loadAccount(uid)

        } ?: sessionExpired()
    }

    /**
     * Load the account referenced from shared preferences accountId
     * @param uid: firebase user uid
     */
    private fun loadAccount(uid: String) {
        val currentAccountId = sharedPreferences.accountId.toString()
        if (currentAccountId.isNotEmpty()) {
            databaseReference.child(currentAccountId).get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        // accountId does not exist in the database
                        getFirstAccount(uid)
                    }
                    else {
                        val account = snapshot.getValue<Account>()
                        account?.let {
                            loadAccountData(uid, it)
                        }
                    }
                }
                .addOnFailureListener {
                    Snackbar
                        .make(binding.dlHome, getString(R.string.load_account_error), 5000)
                        .show()
                }
        }
        else {
            getFirstAccount(uid)
        }
    }

    /**
     * Get the first account found in the database
     * @param uid: firebase user uid
     */
    private fun getFirstAccount(uid: String) {
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val account = child.getValue<Account>()
                    account?.let {
                        sharedPreferences.accountId = it.id
                        sharedPreferences.accountName = it.name
                        sharedPreferences.accountColor = it.color
                        loadAccountData(uid, it)
                    }

                    break
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.dlHome, getString(R.string.load_account_error), 5000)
                    .show()
            }
    }

    /**
     * Set UI data
     * @param uid: Firebase user uid
     * @param account: Account fetched from database
     */
    private fun loadAccountData(uid: String, account: Account) {
        val headerView = binding.nvHome.getHeaderView(0)
        val iconColor = resources.getIdentifier(
            account.color,
            "color",
            this.packageName
        )

        headerView.findViewById<TextView>(R.id.tvHeaderIcon).text =
            account.name?.get(0)?.uppercase()

        headerView.findViewById<TextView>(R.id.tvHeaderIcon).setTextColor(
            ContextCompat.getColor(this, iconColor)
        )

        headerView.findViewById<RelativeLayout>(R.id.rlHeader).setBackgroundColor(
            ContextCompat.getColor(this, iconColor)
        )

        val name =
            account.name?.let {
                if (it.contains(" "))  it.split(" ")[0]
                else it
            }

        headerView.findViewById<TextView>(R.id.tvHeaderName).text = name
        val headerBalance = headerView.findViewById<TextView>(R.id.tvHeaderBalance)

        val budget = "₱" + String.format("%,.2f", account.remainingBalance)
        headerBalance.text = budget

        account.budgetRenewsAt?.let {
            if (isRenewalDate(it)) {
                setAccountRenewalDate(uid, account.id!!, it)
            }
        }
    }

    /**
     * Check if today's date is past renewal date
     * @param date: Renewal date
     */
    private fun isRenewalDate(date: Long): Boolean {
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val today = zdtToday.toInstant().toEpochMilli()
        if (today > date) {
            return true
        }

        return false
    }

    /**
     * Set account's renewal date next month and renew remaining balance
     * @param uid: Firebase user uid
     * @param accountId: Current account's id
     * @param date: Current account's renewal date
     */
    private fun setAccountRenewalDate(uid: String, accountId: String, date: Long) {
        val accountsReference = database.getReference("accounts").child(uid).child(accountId)
        accountsReference.get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()
                account?.let {
                    // set renewal date of account to next month
                    val zdtRenewal = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(date),
                        ZoneId.systemDefault()
                    ).plusMonths(1).toInstant().toEpochMilli()

                    it.budgetRenewsAt = zdtRenewal
                    it.remainingBalance = it.monthlyBudget

                    accountsReference.setValue(it)
                        .addOnSuccessListener {
                            renewMonthlyBudget(uid, accountId)
                        }
                        .addOnFailureListener {
                            Snackbar
                                .make(binding.dlHome, getString(R.string.renew_budget_error), 5000)
                                .show()
                        }
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.dlHome, getString(R.string.renew_budget_error), 5000)
                    .show()
            }
    }

    /**
     * Reset amount spent of all budgets to 0
     * @param uid: Firebase user uid
     * @param accountId: Current account's id
     */
    private fun renewMonthlyBudget(uid: String, accountId: String) {
        val budgetsReference = database.getReference("budgets").child(uid).child(accountId)
        budgetsReference.get()
            .addOnSuccessListener { snapshot ->
                val newBudgets = mutableMapOf<String, Budget>()
                for (child in snapshot.children) {
                    val budget = child.getValue<Budget>()
                    if (budget != null) {
                        budget.amountSpent = 0.0
                        newBudgets[budget.id!!] = budget
                    }
                }

                budgetsReference.setValue(newBudgets)
                    .addOnSuccessListener {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(resources.getString(R.string.budget_renewed_title))
                            .setMessage(resources.getString(R.string.budget_renewed_message))
                            .setPositiveButton(resources.getString(R.string.got_it)) { _, _ -> recreate() }
                            .setOnDismissListener { recreate() }
                            .show()
                    }
                    .addOnFailureListener {
                        Snackbar
                            .make(binding.dlHome, getString(R.string.renew_budget_error), 5000)
                            .show()
                    }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.dlHome, getString(R.string.renew_budget_error), 5000)
                    .show()
            }
    }

    /**
     * User session has expired
     */
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
}