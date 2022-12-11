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
import com.ducatus.adapter.LoanFullyPaidAdapter
import com.ducatus.data.Loan
import com.ducatus.databinding.FragmentLoansFullyPaidBinding
import com.ducatus.interfaces.LoanInterface
import com.ducatus.viewmodel.SearchViewModel2
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

class LoansFullyPaidFragment : Fragment(), LoanInterface {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var binding: FragmentLoansFullyPaidBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var currentAccountId: String
    private lateinit var loanAdapter: LoanFullyPaidAdapter
    private var firebaseUser: FirebaseUser? = null
    private var selectedLoanType: String = "A"
    private var mutableLoans: MutableList<Loan>? = null
    private val searchViewModel2: SearchViewModel2 by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentLoansFullyPaidBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        searchViewModel2.searchInput.observe(viewLifecycleOwner) { name ->
            name.getContentIfNotHandled()?.let { content ->
                searchFullyPaidLoansByName(content.lowercase())
            }
        }

        binding.rgLoansFullyPaid.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLoansFullyPaidAll -> firebaseUser?.let {
                    selectedLoanType = "A"
                    loadFullyPaidLoans("A")
                }
                R.id.rbLoansFullyPaidBorrowed -> firebaseUser?.let {
                    selectedLoanType = "B"
                    loadFullyPaidLoans("B")
                }
                R.id.rbLoansFullyPaidLent -> firebaseUser?.let {
                    selectedLoanType = "L"
                    loadFullyPaidLoans("L")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadFullyPaidLoans(selectedLoanType) }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    firebaseUser?.let {
                        val fragmentManager = childFragmentManager
                        val newFragment = SearchItemDialog2Fragment()
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

    private fun loadFullyPaidLoans(loanType: String) {
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
                    if (loan?.paidAt != null) {
                        loans.add(loan)
                    }
                }

                // sort by newest date
                loans.sortByDescending { it.paidAt!! }
                adaptLoans(loans)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_loans_error), 5000)
                    .show()
            }
    }

    private fun searchFullyPaidLoansByName(name: String) {
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
                    if (loan?.paidAt != null) {
                        loans.add(loan)
                    }
                }

                // search by notes and add to list
                searchFullyPaidLoansByNotes(name, loans)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_loans_error), 5000)
                    .show()
            }
    }

    private fun searchFullyPaidLoansByNotes(name: String, loans: MutableList<Loan>) {
        val query =
            databaseReference
                .orderByChild("notesLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val loan = child.getValue<Loan>()
                    if (loan?.paidAt != null) {
                        if (!loans.contains(loan)) {
                            loans.add(loan)
                        }
                    }
                }

                if (loans.isNotEmpty()) {
                    // sort by newest date
                    loans.sortByDescending { it.paidAt!! }
                    adaptLoans(loans)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "No fully paid loans found with the name $name", Snackbar.LENGTH_LONG)
                        .show()

                    if (mutableLoans?.isNotEmpty() == true) {
                        binding.tvLoansFullyPaidSort.visibility = View.VISIBLE
                        if (loanAdapter.itemCount > 0) {
                            binding.cvLoansFullyPaid.visibility = View.VISIBLE
                        }
                    }
                    else {
                        binding.tvLoansFullyPaidSort.visibility = View.GONE
                        binding.cvLoansFullyPaidEmpty.visibility = View.VISIBLE
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
        loanAdapter = LoanFullyPaidAdapter(mutableListOf(), this)
        binding.rvLoansFullyPaid.adapter = loanAdapter
        binding.rvLoansFullyPaid.layoutManager = LinearLayoutManager(activity)

        for (loan in loans) {
            loanAdapter.addLoan(loan)
        }

        if (loanAdapter.itemCount <= 0) {
            mutableLoans = null
            binding.cvLoansFullyPaidEmpty.visibility = View.VISIBLE
        }
        else {
            mutableLoans = loans
            binding.cvLoansFullyPaid.visibility = View.VISIBLE
            binding.tvLoansFullyPaidSort.visibility = View.VISIBLE
            binding.tvLoansFullyPaidSort.setOnClickListener { showPopup(it) }
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
                        loans.sortBy { it.paidAt!! }
                        adaptLoans(loans)
                    }

                    true
                }
                R.id.sortDueDateNewest -> {
                    mutableLoans?.let { loans ->
                        loans.sortByDescending { it.paidAt!! }
                        adaptLoans(loans)
                    }

                    true
                }
                else -> false
            }
        }

        // menu to inflate
        popup.menuInflater.inflate(R.menu.sort_options_2_menu, popup.menu)
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
        binding.cvLoansFullyPaidEmpty.visibility = View.GONE
        binding.cvLoansFullyPaid.visibility = View.GONE
        binding.tvLoansFullyPaidSort.visibility = View.GONE
        binding.pbLoansFullyPaid.visibility = View.VISIBLE
    }

    private fun hideProgressDialog() {
        binding.pbLoansFullyPaid.visibility = View.INVISIBLE
    }
}