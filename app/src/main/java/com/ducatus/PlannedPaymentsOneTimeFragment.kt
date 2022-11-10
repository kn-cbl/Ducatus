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
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.data.PlannedPayment
import com.ducatus.databinding.FragmentPlannedPaymentsOneTimeBinding
import com.ducatus.viewmodel.SearchViewModel
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

class PlannedPaymentsOneTimeFragment : Fragment(), PlannedPaymentInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentPlannedPaymentsOneTimeBinding
    private lateinit var currentAccountId: String
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private var firebaseUser: FirebaseUser? = null
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentPlannedPaymentsOneTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    firebaseUser?.let {
                        val fragmentManager = childFragmentManager
                        val newFragment = SearchItemDialogFragment()
                        newFragment.show(fragmentManager, "dialog")
                    }
                    true
                }
                else -> false
            }
        }

        searchViewModel.searchInput.observe(viewLifecycleOwner) { name ->
            searchOneTimePlannedPaymentsByCategory(name)
        }
    }

    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadOneTimePlannedPayments() }
    }

    override fun viewItem(plannedPaymentId: String) {
        val intent = Intent(activity, PlannedPaymentDetailActivity::class.java)
        intent.putExtra("plannedPaymentId", plannedPaymentId)
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
            databaseReference = database.getReference("planned_payments").child(firebaseUser!!.uid).child(currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadOneTimePlannedPayments() {
        showProgressDialog()
        val query = databaseReference.orderByChild("date")
        query.get()
            .addOnSuccessListener { snapshot ->
                val plannedPayments = mutableListOf<PlannedPayment>()
                for (child in snapshot.children) {
                    val plannedPayment = child.getValue<PlannedPayment>()
                    if (plannedPayment != null) {
                        if (plannedPayment.frequency == 0) {
                            plannedPayments.add(plannedPayment)
                        }
                    }
                }

                adaptPlannedPayments(plannedPayments)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun searchOneTimePlannedPaymentsByCategory(name: String) {
        showProgressDialog()
        val query = databaseReference.orderByChild("categoryNameLower").startAt(name).endAt(name + "\uf8ff")
        query.get()
            .addOnSuccessListener { snapshot ->
                val plannedPayments = mutableListOf<PlannedPayment>()
                for (child in snapshot.children) {
                    val plannedPayment = child.getValue<PlannedPayment>()
                    if (plannedPayment != null) {
                        if (plannedPayment.frequency == 0) {
                            plannedPayments.add(plannedPayment)
                        }
                    }
                }

                // search by subcategory and add to list
                searchOneTimePlannedPaymentsBySubcategory(name, plannedPayments)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun searchOneTimePlannedPaymentsBySubcategory(name: String, plannedPayments: MutableList<PlannedPayment>) {
        val query = databaseReference.orderByChild("subcategoryNameLower").startAt(name).endAt(name + "\uf8ff")
        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val plannedPayment = child.getValue<PlannedPayment>()
                    if (plannedPayment != null) {
                        if (plannedPayment.frequency == 0) {
                            plannedPayments.add(plannedPayment)
                        }
                    }
                }

                if (plannedPayments.isNotEmpty()) {
                    // sort by date
                    plannedPayments.sortByDescending { it.date!! }
                    adaptPlannedPayments(plannedPayments)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "No one-time planned payments found with the name $name", Snackbar.LENGTH_LONG)
                        .show()
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun adaptPlannedPayments(plannedPayments: MutableList<PlannedPayment>) {
        val plannedPaymentAdapter = PlannedPaymentAdapter(mutableListOf(), this)
        binding.rvPlannedPaymentsOneTime.adapter = plannedPaymentAdapter
        binding.rvPlannedPaymentsOneTime.layoutManager = LinearLayoutManager(activity)

        for (plannedPayment in plannedPayments) {
            plannedPaymentAdapter.addPlannedPayment(plannedPayment)
        }

        if (plannedPaymentAdapter.itemCount <= 0) {
            binding.cvPlannedPaymentsOneTimeEmpty.visibility = View.VISIBLE
        }

        hideProgressDialog()
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
        binding.cvPlannedPaymentsOneTimeEmpty.visibility = View.GONE
        binding.pbPlannedPaymentsOneTime.visibility = View.VISIBLE
        binding.rvPlannedPaymentsOneTime.visibility = View.GONE
        activity.findViewById<FloatingActionButton>(R.id.fabAddPlannedPayment).visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbPlannedPaymentsOneTime.visibility = View.INVISIBLE
        binding.rvPlannedPaymentsOneTime.visibility = View.VISIBLE
        activity.findViewById<FloatingActionButton>(R.id.fabAddPlannedPayment).visibility = View.VISIBLE
    }
}