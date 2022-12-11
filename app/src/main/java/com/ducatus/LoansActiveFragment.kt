package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.LoanActiveAdapter
import com.ducatus.data.Loan
import com.ducatus.databinding.FragmentLoansActiveBinding
import com.ducatus.interfaces.LoanInterface
import com.ducatus.viewmodel.SearchViewModel
import com.google.android.material.appbar.MaterialToolbar
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

class LoansActiveFragment : Fragment(), LoanInterface {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var binding: FragmentLoansActiveBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var currentAccountId: String
    private lateinit var loanAdapter: LoanActiveAdapter
    private lateinit var loanOverdueAdapter: LoanActiveAdapter
    private var firebaseUser: FirebaseUser? = null
    private var selectedLoanType: String = "A"
    private var mutableLoans: MutableList<Loan>? = null
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentLoansActiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        searchViewModel.searchInput.observe(viewLifecycleOwner) { name ->
            name.getContentIfNotHandled()?.let { content ->
                searchActiveLoanByName(content.lowercase())
            }
        }

        binding.rgLoansActive.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLoansActiveAll -> firebaseUser?.let {
                    selectedLoanType = "A"
                    loadActiveLoans("A")
                }
                R.id.rbLoansActiveBorrowed -> firebaseUser?.let {
                    selectedLoanType = "B"
                    loadActiveLoans("B")
                }
                R.id.rbLoansActiveLent -> firebaseUser?.let {
                    selectedLoanType = "L"
                    loadActiveLoans("L")
                }
            }
        }

        binding.fabAddLoan.setOnClickListener {
            startActivity(Intent(activity, LoanAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadActiveLoans(selectedLoanType) }

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
    }

    override fun viewItem(loanId: String) {
        val intent = Intent(activity, LoanDetailActivity::class.java)
        intent.putExtra("loanId", loanId)
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
            databaseReference =
                database
                    .getReference("loans")
                    .child(firebaseUser!!.uid)
                    .child(currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadActiveLoans(loanType: String) {
        showProgressDialog()
        val query =
            if (loanType == "L" || loanType == "B") { // borrowed/lent loans
                databaseReference.orderByChild("type").equalTo(loanType)
            }
            else { // default; all loans
                databaseReference
            }

        query.get()
            .addOnSuccessListener { snapshot ->
                val loans = mutableListOf<Loan>()
                for (child in snapshot.children) {
                    val loan = child.getValue<Loan>()
                    if (loan != null && loan.paidAt == null) {
                        loans.add(loan)
                    }
                }

                // sort by oldest date
                loans.sortBy { it.dueDate!! }
                adaptLoans(loans)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_loans_error), 5000)
                    .show()
            }
    }

    private fun searchActiveLoanByName(name: String) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("nameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                val loans = mutableListOf<Loan>()
                for (child in snapshot.children) {
                    val loan = child.getValue<Loan>()
                    if (loan != null && loan.paidAt == null) {
                        loans.add(loan)
                    }
                }

                // search by notes and add to list
                searchActiveLoanByNotes(name, loans)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_loans_error), 5000)
                    .show()
            }
    }

    private fun searchActiveLoanByNotes(name: String, loans: MutableList<Loan>) {
        val query =
            databaseReference
                .orderByChild("notesLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val loan = child.getValue<Loan>()
                    if (loan != null && loan.paidAt == null) {
                        if (!loans.contains(loan)) {
                            loans.add(loan)
                        }
                    }
                }

                if (loans.isNotEmpty()) {
                    // sort by oldest date
                    loans.sortBy { it.dueDate!! }
                    adaptLoans(loans)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "No active loans found with the name $name", Snackbar.LENGTH_LONG)
                        .show()

                    if (mutableLoans?.isNotEmpty() == true) {
                        binding.tvLoansActiveSort.visibility = View.VISIBLE
                        if (loanAdapter.itemCount > 0) {
                            binding.cvLoansActive.visibility = View.VISIBLE
                            binding.tvLoansActive.visibility = View.VISIBLE
                        }
                        if (loanOverdueAdapter.itemCount > 0) {
                            binding.cvLoansActiveOverdue.visibility = View.VISIBLE
                            binding.tvLoansActiveOverdue.visibility = View.VISIBLE
                        }
                    }
                    else {
                        binding.tvLoansActiveSort.visibility = View.GONE
                        binding.cvLoansActiveEmpty.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_loans_error), 5000)
                    .show()
            }
    }

    private fun adaptLoans(loans: MutableList<Loan>) {
        loanAdapter = LoanActiveAdapter(mutableListOf(), this)
        binding.rvLoansActive.adapter = loanAdapter
        binding.rvLoansActive.layoutManager = LinearLayoutManager(activity)

        loanOverdueAdapter = LoanActiveAdapter(mutableListOf(), this)
        binding.rvLoansActiveOverdue.adapter = loanOverdueAdapter
        binding.rvLoansActiveOverdue.layoutManager = LinearLayoutManager(activity)

        for (loan in loans) {
            // determine if loan is overdue or not
            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )

            if (zdt.toInstant().toEpochMilli() < loan.dueDate!!) {
                loanAdapter.addLoan(loan)
            }
            else {
                loanOverdueAdapter.addLoan(loan)
            }
        }

        if (loanAdapter.itemCount > 0) {
            binding.cvLoansActive.visibility = View.VISIBLE
            binding.tvLoansActive.visibility = View.VISIBLE
        }

        if (loanOverdueAdapter.itemCount > 0) {
            binding.cvLoansActiveOverdue.visibility = View.VISIBLE
            binding.tvLoansActiveOverdue.visibility = View.VISIBLE
        }

        if (loanAdapter.itemCount <= 0 && loanOverdueAdapter.itemCount <= 0) {
            mutableLoans = null
            binding.cvLoansActiveEmpty.visibility = View.VISIBLE
        }
        else {
            mutableLoans = loans
            binding.tvLoansActiveSort.visibility = View.VISIBLE
            binding.tvLoansActiveSort.setOnClickListener { showPopup(it) }
        }

        hideProgressDialog()
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sortAmountLowest -> {
                    mutableLoans?.let { loans ->
                        loans.sortBy { it.amount }
                        adaptLoans(loans)
                    }

                    true
                }
                R.id.sortAmountHighest -> {
                    mutableLoans?.let { loans ->
                        loans.sortByDescending { it.amount }
                        adaptLoans(loans)
                    }

                    true
                }
                R.id.sortDueDateOldest -> {
                    mutableLoans?.let { loans ->
                        loans.sortBy { it.dueDate!! }
                        adaptLoans(loans)
                    }

                    true
                }
                R.id.sortDueDateNewest -> {
                    mutableLoans?.let { loans ->
                        loans.sortByDescending { it.dueDate!! }
                        adaptLoans(loans)
                    }

                    true
                }
                else -> false
            }
        }

        // menu to inflate
        popup.menuInflater.inflate(R.menu.sort_options_3_menu, popup.menu)
        popup.show()
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

    private fun showProgressDialog() {
        binding.pbLoansActive.visibility = View.VISIBLE
        binding.cvLoansActiveEmpty.visibility = View.GONE
        binding.cvLoansActive.visibility = View.GONE
        binding.cvLoansActiveOverdue.visibility = View.GONE
        binding.tvLoansActiveSort.visibility = View.GONE
        binding.tvLoansActive.visibility = View.GONE
        binding.tvLoansActiveOverdue.visibility = View.GONE
        binding.fabAddLoan.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbLoansActive.visibility = View.INVISIBLE
        binding.fabAddLoan.visibility = View.VISIBLE
    }
}