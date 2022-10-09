package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.Navigation
import com.ducatus.databinding.ActivityHomeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding
    private var currentFragment: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()
        networkObserver()

        setSupportActionBar(binding.tbHome)

        binding.tbHome.setNavigationOnClickListener {
            binding.dlHome.open()
        }

//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction().replace(binding.fcHome.id, HomeFragment()).commit()
//            binding.nvHome.setCheckedItem(R.id.nav_home)
//        }

        binding.nvHome.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    currentFragment = R.id.homeFragment
                    binding.tbHome.setTitle(R.string.home)

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigateUp()
                    action.navigate(R.id.homeFragment)
                }
//                R.id.nav_reports -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, ReportsFragment()).commit()
//                }
                R.id.nav_budgets -> {
                    currentFragment = R.id.budgetsFragment
                    binding.tbHome.setTitle(R.string.budgets)

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigateUp()
                    action.navigate(R.id.budgetsFragment)
                }
//                R.id.nav_transactions -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, TransactionsFragment()).commit()
//                }
//                R.id.nav_planned_payments -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, PlannedPaymentsFragment()).commit()
//                }
//                R.id.nav_loans -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, LoansFragment()).commit()
//                }
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
                    currentFragment = R.id.settingsFragment
                    binding.tbHome.setTitle(R.string.settings)

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigateUp()
                    action.navigate(R.id.settingsFragment)
                }
            }

            menuItem.isChecked = true
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
        loadAccount()
    }

    private fun loadData() {
        currentFragment = R.id.homeFragment

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            loadAccount()
        }
        else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun loadAccount() {
        showProgressDialog()
        val sharedPreferences = SharedPreferences(this)
        val currentAccountName = sharedPreferences.accountName
        val currentAccountColor = sharedPreferences.accountColor

        val headerView = binding.nvHome.getHeaderView(0)
        val imageColor = resources.getIdentifier(
            currentAccountColor,
            "color",
            this.packageName
        )

        headerView.findViewById<ImageView>(R.id.ivHeaderImage).setColorFilter(
            ResourcesCompat.getColor(
                resources,
                R.color.white,
                null
            )
        )
        headerView.findViewById<RelativeLayout>(R.id.rlHeader).setBackgroundColor(ContextCompat.getColor(this, imageColor))
        headerView.findViewById<TextView>(R.id.tvHeaderName).text = currentAccountName

        hideProgressDialog()
    }

    private fun networkObserver() {
        var connectionStatus: Boolean = true
//        val snackbarAvailable = Snackbar.make(binding.dlHome, getString(R.string.connection_available), Snackbar.LENGTH_LONG)
        val snackbarUnavailable = Snackbar.make(binding.dlHome, getString(R.string.connection_unavailable), Snackbar.LENGTH_INDEFINITE)

        NetworkConnectivityObserver(this).observe(this) {
            if (it == NetworkStatus.Available) {
                // connection status prevents continuous display of snackbar
                if (!connectionStatus) {
                    connectionStatus = true
                    snackbarUnavailable.dismiss()
//                    snackbarAvailable.show()
                }
            }
            else if (it == NetworkStatus.Unavailable) {
                connectionStatus = false
                snackbarUnavailable.show()

                // add 3 second delay to double check network connectivity
//                object : CountDownTimer(3000, 1000) {
//                    override fun onTick(millisUntilFinished: Long) {
//                        // do nothing
//                    }
//                    override fun onFinish() {
//                        if (it == NetworkStatus.Unavailable) {
//
//                        }
//                    }
//                }.start()
            }
        }
    }

    private fun showProgressDialog() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

//    private fun replaceFragmentAnimation(fragment: Fragment) {
//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//        transaction.replace(binding.fcHome.id, fragment)
//        transaction.addToBackStack(null)
//        transaction.commit()
//    }
}