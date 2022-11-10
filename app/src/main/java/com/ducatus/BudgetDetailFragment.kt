package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.data.Budget
import com.ducatus.data.Transaction
import com.ducatus.databinding.FragmentBudgetDetailBinding
import com.ducatus.viewmodel.BudgetViewModel
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

class BudgetDetailFragment : Fragment(), TransactionHistoryInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentBudgetDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var budgetId: String
    private lateinit var currentAccountId: String
    private var firebaseUser: FirebaseUser? = null
    private val budgetViewModel: BudgetViewModel by activityViewModels()
    private var updated: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llBudgetDetailRoot)
        binding = FragmentBudgetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        budgetViewModel.isUpdated.observe(viewLifecycleOwner) { isUpdated ->
            updated = isUpdated
            if (updated) {
                firebaseUser?.let { loadBudget(it.uid, currentAccountId, budgetId) }
            }
        }
    }

    override fun getActivityInterface(): Activity {
        return activity
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()
            budgetId = activity.intent.getStringExtra("budgetId").toString()
            database = Firebase.database

            loadBudget(firebaseUser!!.uid, currentAccountId, budgetId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadBudget(uid: String, accountId: String, budgetId: String) {
        showProgressDialog()
        databaseReference = database.getReference("budgets").child(uid).child(accountId).child(budgetId)
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue<Budget>()
                if (budget != null) {
                    loadTransactionHistory(uid, accountId, budget.id.toString())

                    val iconColor = resources.getIdentifier(
                        budget.categoryColor,
                        "color",
                        activity.packageName
                    )

                    binding.flBudgetDetailCategoryIcon.backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

                    val icon = resources.getIdentifier(
                        budget.categoryIcon,
                        "drawable",
                        activity.packageName
                    )

                    binding.ivBudgetDetailCategoryIcon.setImageResource(icon)
                    binding.ivBudgetDetailCategoryIcon.setColorFilter(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.white,
                            null
                        )
                    )

                    val budgetData = mapOf(
                        "name" to budget.name!!,
                        "total" to budget.amountTotal.toString(),
                        "spent" to budget.amountSpent.toString(),
                        "categoryName" to budget.categoryName!!
                    )

                    binding.ibBudgetDetailEdit.setOnClickListener {
                        showPopup(
                            it,
                            uid,
                            accountId,
                            budgetData
                        )
                    }

                    binding.tvBudgetDetailName.text = budget.name
                    binding.tvBudgetDetailCategory.text = budget.categoryName

                    val budgetTotal = budget.amountTotal.toString().toDouble()
                    val budgetSpent = budget.amountSpent.toString().toDouble()
                    val budgetLeft = budgetTotal.minus(budgetSpent)

                    val spentText = "₱" + String.format("%,.2f", budgetSpent)
                    binding.tvBudgetDetailSpent.text = spentText
                    binding.tvBudgetDetailSpent.setTextColor(
                        ContextCompat.getColor(activity, iconColor)
                    )

                    val budgetLeftText = "₱" + String.format("%,.2f", budgetLeft)
                    binding.tvBudgetDetailLeft.text = budgetLeftText
                    binding.tvBudgetDetailLeft.setTextColor(
                        ContextCompat.getColor(activity, iconColor)
                    )

                    val budgetTotalText = "₱" + String.format("%,.2f", budgetTotal)
                    binding.tvBudgetDetailLimit.text = budgetTotalText
                    binding.tvBudgetDetailLimit.setTextColor(
                        ContextCompat.getColor(activity, iconColor)
                    )

                    binding.pbBudgetDetailStatus.progress = ((budgetSpent / budgetTotal) * 100).toInt()
                    binding.pbBudgetDetailStatus.setIndicatorColor(ContextCompat.getColor(activity, iconColor))

                    // determine icon and text to display
                    var statusIcon = ""
                    var statusText = ""

                    when (binding.pbBudgetDetailStatus.progress) {
                        in 0..59 -> {
                            statusIcon = "ic_budget_status_1"
                            statusText = "Your budget is on track"
                        }
                        in 60..99 -> {
                            statusIcon = "ic_budget_status_2"
                            statusText = "You have almost reached your budget limit"
                        }
                        100 -> {
                            statusIcon = "ic_budget_status_3"
                            statusText = "You have reached your budget limit"
                        }
                    }

                    val statusIconRes = resources.getIdentifier(
                        statusIcon,
                        "drawable",
                        activity.packageName
                    )

                    binding.ivBudgetDetailStatus.setImageResource(statusIconRes)
                    binding.tvBudgetDetailStatus.text = statusText
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun loadTransactionHistory(uid: String, accountId: String, categoryId: String) {
        showProgressDialog()
        databaseReference = database.getReference("transactions").child(uid).child(accountId)
        val query = databaseReference.orderByChild("categoryId").equalTo(categoryId)
        query.get()
            .addOnSuccessListener { snapshot ->
                val transactionHistoryAdapter = TransactionHistoryAdapter(mutableListOf(), this)
                binding.rvTransactionHistory.adapter = transactionHistoryAdapter
                binding.rvTransactionHistory.layoutManager = LinearLayoutManager(activity)

                for (child in snapshot.children) {
                    val transaction = child.getValue<Transaction>()
                    if (transaction != null) {
                        transactionHistoryAdapter.addTransaction(transaction)
                    }
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun showPopup(view: View, uid: String, accountId: String, budgetData: Map<String, String>) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.optionEdit -> {
                    val action =
                        BudgetDetailFragmentDirections
                            .actionBudgetDetailFragmentToBudgetEditDialogFragment(
                                budgetId,
                                budgetData["name"]!!,
                                budgetData["total"]!!,
                                budgetData["spent"]!!,
                                budgetData["categoryName"]!!,
                            )

                    findNavController().navigate(action)
                    true
                }
                R.id.optionDelete-> {
                    confirmDelete(uid, accountId, budgetId)
                    true
                }
                else -> false
            }
        }
        popup.menuInflater.inflate(R.menu.edit_options_2_menu, popup.menu)
        popup.show()
    }

    private fun confirmDelete(uid: String, accountId: String, categoryId: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.delete_budget))
            .setMessage(resources.getString(R.string.delete_budget_confirm))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteBudget(uid, accountId, categoryId) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteBudget(uid: String, accountId: String, categoryId: String) {
        showProgressDialog()
        databaseReference = database.getReference("budgets").child(uid).child(accountId).child(budgetId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                unallocateCategory(uid, accountId, categoryId)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!,5000)
                    .show()
            }
    }

    private fun unallocateCategory(uid: String, accountId: String, categoryId: String) {
        showProgressDialog()
        databaseReference = database.getReference("categories").child(uid).child(accountId).child(categoryId)
        databaseReference.child("allocated").setValue(false)
            .addOnSuccessListener {
                hideProgressDialog()
                activity.onBackPressed()
            }
            .addOnFailureListener {
                hideProgressDialog()
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
        binding.pbBudgetDetail.visibility = View.VISIBLE
        binding.llBudgetDetail.visibility = View.GONE
        binding.rvTransactionHistory.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbBudgetDetail.visibility = View.INVISIBLE
        binding.llBudgetDetail.visibility = View.VISIBLE
        binding.rvTransactionHistory.visibility = View.VISIBLE
    }
}