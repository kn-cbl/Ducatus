package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
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
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadData()

//        networkObserver()

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
//                R.id.nav_goals -> {
//                    startActivity(Intent(this, EditGoal::class.java))
//                }
//                R.id.nav_challenges -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, ChallengesFragment()).commit()
//                }
//                R.id.nav_tips -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, TipsFragment()).commit()
//                }
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

    private fun hasNotification() {
        val notificationIntent = intent.getStringExtra("notification")
        if (notificationIntent != null) {
            val accountId = intent.getStringExtra("accountId")
            val itemId = intent.getStringExtra("itemId")

            when (notificationIntent) {
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

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            loadAccount(firebaseUser!!.uid)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccount(uid: String) {
        val sharedPreferences = SharedPreferences(this)
        val currentAccountId = sharedPreferences.accountId.toString()
        val currentAccountName = sharedPreferences.accountName
        val currentAccountColor = sharedPreferences.accountColor

        val headerView = binding.nvHome.getHeaderView(0)
        val iconColor = resources.getIdentifier(
            currentAccountColor,
            "color",
            this.packageName
        )

        headerView.findViewById<TextView>(R.id.tvHeaderIcon).text = currentAccountName?.get(0)?.uppercase()
        headerView.findViewById<TextView>(R.id.tvHeaderIcon).setTextColor(
            ContextCompat.getColor(this, iconColor)
        )

        headerView.findViewById<RelativeLayout>(R.id.rlHeader).setBackgroundColor(
            ContextCompat.getColor(this, iconColor)
        )

        val name =
            currentAccountName?.let {
                if (it.contains(" "))  it.split(" ")[0]
                else it
            }

        headerView.findViewById<TextView>(R.id.tvHeaderName).text = name
        val headerBalance = headerView.findViewById<TextView>(R.id.tvHeaderBalance)

        database = Firebase.database
        loadAccountRemainingBalance(uid, currentAccountId, headerBalance)
    }

    private fun loadAccountRemainingBalance(uid: String, accountId: String, headerBalance: TextView) {
        databaseReference = database.getReference("accounts").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()
                if (account != null) {
                    val budget = "₱" + String.format("%,.2f", account.remainingBalance)
                    headerBalance.text = budget

                    account.budgetRenewsAt?.let {
                        if (isRenewalDate(it)) {
                            setAccountRenewalDate(uid, accountId, it)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.dlHome, getString(R.string.load_account_error), 5000)
                    .show()
            }
    }

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

    private fun setAccountRenewalDate(uid: String, accountId: String, date: Long) {
        // set renewal date of account to next month
        val zdtRenewal = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneId.systemDefault()
        )
        val nextMonth = zdtRenewal.plusMonths(1).toInstant().toEpochMilli()
        databaseReference.child("budgetRenewsAt").setValue(nextMonth)
            .addOnSuccessListener {
                renewMonthlyBudget(uid, accountId)
            }
            .addOnFailureListener {
                Snackbar
                    .make(binding.dlHome, getString(R.string.renew_budget_error), 5000)
                    .show()
            }
    }

    private fun renewMonthlyBudget(uid: String, accountId: String) {
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val newBudgets = mutableMapOf<String, Budget>()
                for (child in snapshot.children) {
                    val budget = child.getValue<Budget>()
                    if (budget != null) {
                        budget.amountSpent = 0.0
                        newBudgets[budget.id!!] = budget
                    }
                }

                databaseReference.setValue(newBudgets)
                    .addOnSuccessListener {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(resources.getString(R.string.budget_renewed_title))
                            .setMessage(resources.getString(R.string.budget_renewed_message))
                            .setPositiveButton(resources.getString(R.string.got_it)) { _, _ -> }
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

    private fun networkObserver() {
        var activityStarted = false

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                activityStarted = true
            }
        }.start()

        if (activityStarted) {
            val snackbarAvailable = Snackbar.make(binding.dlHome, getString(R.string.connection_available), Snackbar.LENGTH_LONG)
            val snackbarUnavailable = Snackbar.make(binding.dlHome, getString(R.string.connection_unavailable), Snackbar.LENGTH_INDEFINITE)

            NetworkConnectivityObserver(this).observe(this) {
                if (it == NetworkStatus.Available) {
                    snackbarUnavailable.dismiss()
                    snackbarAvailable.show()
                }
                else if (it == NetworkStatus.Unavailable) {
                    snackbarUnavailable.show()
                }
            }
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

//    private fun replaceFragmentAnimation(fragment: Fragment) {
//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//        transaction.replace(binding.fcHome.id, fragment)
//        transaction.addToBackStack(null)
//        transaction.commit()
//    }
}