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
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.data.Budget
import com.ducatus.databinding.FragmentBudgetsBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class BudgetsFragment : Fragment(), BudgetInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentBudgetsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var currentAccountId: String
    private var firebaseUser: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        binding = FragmentBudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.fabAddBudget.setOnClickListener {
            startActivity(Intent(activity, BudgetAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadBudgets(it.uid, currentAccountId) }
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem(budgetId: String) {
        val intent = Intent(activity, BudgetDetailActivity::class.java)
        intent.putExtra("budgetId", budgetId)
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()
            database = Firebase.database
        }
        else {
            sessionExpired()
        }
    }

    private fun loadBudgets(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        val query = databaseReference.orderByChild("name")
        query.get()
            .addOnSuccessListener { snapshot ->
                val budgetAdapter = BudgetAdapter(mutableListOf(), this)
                binding.rvBudgets.adapter = budgetAdapter
                binding.rvBudgets.layoutManager = LinearLayoutManager(activity)

                for (child in snapshot.children) {
                    val budget = child.getValue<Budget>()
                    if (budget != null) {
                        budgetAdapter.addBudget(budget)
                    }
                }

                if (budgetAdapter.itemCount > 0) {
                    getCategoryCount(uid, accountId, budgetAdapter)
                }
                else {
                    binding.cvBudgetsEmpty.visibility = View.VISIBLE
                    hideProgressDialog()
                }

            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun getCategoryCount(uid: String, accountId: String, adapter: BudgetAdapter) {
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val categoryCount = snapshot.childrenCount
                if (adapter.itemCount >= categoryCount) {
                    binding.fabAddBudget.visibility = View.GONE
                }
                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                try {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.cvBudgetsEmpty.visibility = View.GONE
        binding.pbBudgets.visibility = View.VISIBLE
        binding.rvBudgets.visibility = View.GONE
        binding.fabAddBudget.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbBudgets.visibility = View.INVISIBLE
        binding.rvBudgets.visibility = View.VISIBLE
        binding.fabAddBudget.visibility = View.VISIBLE
    }
}