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
import com.ducatus.data.Loan
import com.ducatus.databinding.FragmentLoansActiveBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class LoansActiveFragment : Fragment(), LoanInterface {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var binding: FragmentLoansActiveBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var loanAdapter: LoanAdapter
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var currentAccountId: String
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        loadData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        toolbar = activity.findViewById(R.id.tbHome)
        binding = FragmentLoansActiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    firebaseUser?.let { searchActiveLoan() }
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadActiveLoans(it.uid, currentAccountId) }
    }

    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem(loanId: String) {
        TODO("Not yet implemented")
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database

            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()

            loanAdapter = LoanAdapter(mutableListOf(), this)
            binding.rvLoansActive.adapter = loanAdapter
            binding.rvLoansActive.layoutManager = LinearLayoutManager(activity)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadActiveLoans(uid: String, accountId: String) {
        showProgressDialog()
        databaseReference = database.getReference("loans").child(uid).child(accountId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val loans = mutableListOf<Loan>()
                for (child in snapshot.children) {
                    val loan = child.getValue<Loan>()
                    if (loan != null) {
                        if (loan.status == 0) {
                            loans.add(loan)
                        }
                    }
                }

                loans.sortByDescending {
                    it.date!! + it.hour!! + it.minute!!
                }

                for (item in loans) {
                    loanAdapter.addLoan(item)
                }

                if (loanAdapter.itemCount <= 0) {
                    binding.cvLoansActiveEmpty.visibility = View.VISIBLE
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun searchActiveLoan() {
        showProgressDialog()
        loanAdapter = LoanAdapter(mutableListOf(), this)
        binding.rvLoansActive.adapter = loanAdapter
        binding.rvLoansActive.layoutManager = LinearLayoutManager(activity)

        databaseReference.get()
            .addOnSuccessListener {

            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
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
        binding.cvLoansActiveEmpty.visibility = View.GONE
        binding.pbLoansActive.visibility = View.VISIBLE
        binding.rvLoansActive.visibility = View.GONE
        activity.findViewById<FloatingActionButton>(R.id.fabAddLoan).visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbLoansActive.visibility = View.INVISIBLE
        binding.rvLoansActive.visibility = View.VISIBLE
        activity.findViewById<FloatingActionButton>(R.id.fabAddLoan).visibility = View.VISIBLE
    }
}