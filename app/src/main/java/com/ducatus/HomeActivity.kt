package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.navigation.Navigation
import com.ducatus.databinding.ActivityHomeBinding
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

        loadUser()

//        NetworkConnectivityObserver(this).observe(this) {
//            if (it == NetworkStatus.Available) {
//                val action = Navigation.findNavController(this, R.id.fcHome)
//                action.navigateUp()
//                action.navigate(currentFragment)
//            }
//            else {
//                val action = Navigation.findNavController(this, R.id.fcHome)
//                action.navigateUp()
//                action.navigate(R.id.noConnectionFragment)
//            }
//        }

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
//                R.id.nav_transactions -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, TransactionsFragment()).commit()
//                }
//                R.id.nav_reports -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, ReportsFragment()).commit()
//                }
//                R.id.nav_planned_payments -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, PlannedPaymentsFragment()).commit()
//                }
//                R.id.nav_budgets -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, BudgetsFragment()).commit()
//                }
//                R.id.nav_loans -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, LoansFragment()).commit()
//                }
//                R.id.nav_goals -> {
//                    supportFragmentManager.beginTransaction().replace(binding.fcHome.id, GoalsFragment()).commit()
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

    private fun loadUser() {
        currentFragment = R.id.homeFragment

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val headerView = binding.nvHome.getHeaderView(0)
            val username = headerView.findViewById<TextView>(R.id.tvUsername)
            username.text = firebaseUser.displayName
        }
        else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

//    private fun replaceFragmentAnimation(fragment: Fragment) {
//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//        transaction.replace(binding.fcHome.id, fragment)
//        transaction.addToBackStack(null)
//        transaction.commit()
//    }
}