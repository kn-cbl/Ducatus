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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

//        networkObserver()

        setSupportActionBar(binding.tbHome)
        binding.tbHome.setNavigationOnClickListener {
            binding.dlHome.open()
        }

        binding.nvHome.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    binding.tbHome.setTitle(R.string.home)

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigateUp()
                    action.navigate(R.id.homeFragment)
                }
//                R.id.nav_reports -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, ReportsFragment()).commit()
//                }
                R.id.nav_budgets -> {
                    binding.tbHome.setTitle(R.string.budgets)

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigateUp()
                    action.navigate(R.id.budgetsFragment)
                }
                R.id.nav_transactions -> {
                    binding.tbHome.setTitle(R.string.transactions)

                    val action = Navigation.findNavController(this, R.id.fcHome)
                    action.navigateUp()
                    action.navigate(R.id.transactionsFragment)
                }
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
        loadData()
    }

    private fun loadData() {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            loadAccount()
        }
        else {
            sessionExpired()
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
        showProgressDialog()
        Snackbar
            .make(binding.dlHome, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }.start()
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