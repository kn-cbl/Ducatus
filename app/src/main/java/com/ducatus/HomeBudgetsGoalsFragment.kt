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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.HomeBudgetAdapter
import com.ducatus.data.Budget
import com.ducatus.databinding.FragmentHomeBudgetsGoalsBinding
import com.ducatus.interfaces.HomeBudgetsGoalsInterface
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class HomeBudgetsGoalsFragment : Fragment(), HomeBudgetsGoalsInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentHomeBudgetsGoalsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var navigationView: NavigationView
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var currentAccountId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        navigationView = activity.findViewById(R.id.nvHome)
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentHomeBudgetsGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.tvHomeViewBudgets.setOnClickListener {
            toolbar.menu.clear()
            toolbar.setTitle(R.string.budgets)
            val budgetItem = navigationView.menu.findItem(R.id.nav_budgets)
            budgetItem.isChecked = true

            val action = HomeFragmentDirections.actionHomeFragmentToBudgetsFragment()
            findNavController().navigate(action)
        }

//        binding.tvViewGoals.setOnClickListener {
//            toolbar.setTitle(R.string.goals)
//            val goalItem = navigationView.menu.findItem(R.id.nav_goals)
//            goalItem.isChecked = true
//
//            val action = HomeFragmentDirections.actionHomeFragmentToGoalsFragment()
//            findNavController().navigate(action)
//        }
    }

    override fun viewItem(gsonObject: String, type: String) {
        when (type) {
            "B" -> {
                val intent = Intent(activity, BudgetDetailActivity::class.java)
                intent.putExtra("budget", gsonObject)
                startActivity(intent)
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            "G" -> {

            }
        }
    }

    private fun loadData() {
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            loadRecentBudgets(firebaseUser.uid, currentAccountId)
            loadRecentGoals(firebaseUser.uid, currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadRecentBudgets(uid: String, accountId: String) {
        showProgressDialogBudgets()
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val homeBudgetAdapter = HomeBudgetAdapter(mutableListOf(), this@HomeBudgetsGoalsFragment)
                binding.rvHomeRecentBudgets.adapter = homeBudgetAdapter
                binding.rvHomeRecentBudgets.layoutManager = LinearLayoutManager(activity)

                val budgets = mutableListOf<Budget>()
                for (child in snapshot.children) {
                    val budget = child.getValue<Budget>()
                    if (budget != null) {
                        budgets.add(budget)
                    }
                }

                // sort by latest update
                budgets.sortByDescending { it.updatedAt }

                // limit to 3 items only
                val size =
                    if (budgets.size <= 3) budgets.size
                    else 3

                for (i in 0 until size) {
                    homeBudgetAdapter.addBudget(budgets[i])
                }

                if (homeBudgetAdapter.itemCount <= 0) {
                    binding.tvHomeRecentBudgetsEmpty.visibility = View.VISIBLE
                }

                hideProgressDialogBudgets()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_budgets_error),5000)
                    .show()
            }
    }

    private fun loadRecentGoals(uid: String, accountId: String) {
        showProgressDialogGoals()
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

    private fun showProgressDialogBudgets() {
        binding.pbHomeRecentBudgets.visibility = View.VISIBLE
        binding.rvHomeRecentBudgets.visibility = View.GONE
    }

    private fun hideProgressDialogBudgets() {
        binding.pbHomeRecentBudgets.visibility = View.INVISIBLE
        binding.rvHomeRecentBudgets.visibility = View.VISIBLE
    }

    private fun showProgressDialogGoals() {
        binding.pbHomeRecentGoals.visibility = View.VISIBLE
        binding.rvHomeRecentGoals.visibility = View.GONE
    }

    private fun hideProgressDialogGoals() {
        binding.pbHomeRecentGoals.visibility = View.INVISIBLE
        binding.rvHomeRecentGoals.visibility = View.VISIBLE
    }
}