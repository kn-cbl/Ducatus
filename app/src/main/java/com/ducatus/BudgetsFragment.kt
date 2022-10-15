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
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
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

        binding.ibAddBudget.setOnClickListener {
            startActivity(Intent(activity, BudgetAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem() {
        TODO()
    }

    private fun loadData() {
        activity = requireActivity()
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            budgetAdapter = BudgetAdapter(mutableListOf(), this)
            binding.rvBudgets.adapter = budgetAdapter
            binding.rvBudgets.layoutManager = LinearLayoutManager(activity)

            database = Firebase.database
            loadBudgets(firebaseUser!!.uid, currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadBudgets(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("budgets").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener {
                for (child in it.children) {
                    val budget = child.getValue<Budget>()
                    if (budget != null) {
                        budgetAdapter.addBudget(budget)
                    }
                }

                if (budgetAdapter.itemCount <= 0) binding.cvBudgetsEmpty.visibility = View.VISIBLE

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, "Unable to load data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { loadBudgets(uid, accountId) }
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
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.cvBudgetsEmpty.visibility = View.GONE
        binding.pbBudgets.visibility = View.VISIBLE
        binding.rvBudgets.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbBudgets.visibility = View.INVISIBLE
        binding.rvBudgets.visibility = View.VISIBLE
    }
}