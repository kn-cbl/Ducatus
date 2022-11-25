package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ducatus.data.Account
import com.ducatus.databinding.FragmentAccountAddBinding
import com.ducatus.viewmodel.ColorViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class AccountAddFragment : Fragment() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountAddBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private val colorViewModel: ColorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        toolbar = activity.findViewById(R.id.tbAccounts)
        toolbar.title = getString(R.string.add_account)
        toolbar.inflateMenu(R.menu.check_menu)

        binding = FragmentAccountAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputObserver()
        setAmountPresetClickListener()

        colorViewModel.color.observe(viewLifecycleOwner) { selectedColor ->
            setColor(selectedColor)
        }

        binding.tfAddAccountColor.editText?.setOnClickListener {
            val fragmentManager = childFragmentManager
            val newFragment = ColorDialogFragment()
            newFragment.show(fragmentManager, "dialog")
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    // validate data -> check if account exists -> add account, create categories
                    validateData()
                    true
                }
                else -> false
            }
        }
    }

    private fun setAmountPresetClickListener() {
        val amountList = listOf(
            "1000", "2000", "5000",
            "10000", "20000", "30000",
            "50000", "75000", "100000"
        )

        val gridLayout = activity.findViewById<GridLayout>(R.id.glAmountPreset)
        for (i in 0 until gridLayout.childCount) {
            val gridItem = gridLayout.getChildAt(i) as TextView
            gridItem.text = amountList[i]
            gridItem.tag = amountList[i]

            gridLayout.getChildAt(i).setOnClickListener { item ->
                val amount = item.tag.toString()
                binding.tfAddAccountBudget.editText?.setText(amount)
            }
        }
    }

    private fun setColor(selectedColor: String) {
        val color = resources.getIdentifier(
            selectedColor,
            "color",
            activity.packageName
        )

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(activity.getColor(color))
        gradientDrawable.cornerRadius = 16f

        binding.viewAddAccountSelectedColor.background = gradientDrawable
        binding.tfAddAccountColor.tag = selectedColor
        binding.tfAddAccountColor.error = null
    }

    private fun inputObserver() {
        binding.tfAddAccountName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfAddAccountName.error = getString(R.string.account_name_empty)
            else binding.tfAddAccountName.error = null
        }
        binding.tfAddAccountBudget.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfAddAccountBudget.error = getString(R.string.monthly_budget_empty)
            else binding.tfAddAccountBudget.error = null
        }
        binding.tfAddAccountBudget.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
        }
    }

    private fun validateData() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val accountName = binding.tfAddAccountName.editText?.text.toString().trim {it <= ' '}
        val accountMonthlyBudget = binding.tfAddAccountBudget.editText?.text.toString().trim {it <= ' '}
        val accountColor = binding.tfAddAccountColor.tag
        var errors = 0

        if (TextUtils.isEmpty(accountName)) {
            binding.tfAddAccountName.error = getString(R.string.account_name_empty)
            errors++
        }
        if (TextUtils.isEmpty(accountMonthlyBudget)) {
            binding.tfAddAccountBudget.error = getString(R.string.monthly_budget_empty)
            errors++
        }
        if (accountMonthlyBudget.startsWith("0")) {
            binding.tfAddAccountBudget.error = getString(R.string.amount_starts_0)
            errors++
        }
        if (accountColor == null) {
            binding.tfAddAccountColor.error = getString(R.string.select_a_color)
            errors++
        }

        if (errors == 0) {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                accountExists(
                    firebaseUser.uid,
                    accountName,
                    accountMonthlyBudget.toDouble(),
                    accountColor.toString()
                )
            }
            else {
                sessionExpired()
            }
        }
    }

    private fun accountExists(uid: String, accountName: String, accountMonthlyBudget: Double, accountColor: String) {
        showProgressDialog()
        database = Firebase.database
        databaseReference = database.getReference("accounts").child(uid)
        val query = databaseReference.orderByChild("nameLower").equalTo(accountName)
        query.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // get next month epoch time
                    val zdtToday = ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneId.systemDefault()
                    )
                    val nextMonth = zdtToday.plusMonths(1).toInstant().toEpochMilli()

                    val key = databaseReference.push().key
                    val account = Account(
                        key,
                        accountName,
                        accountName.lowercase(),
                        accountColor,
                        accountMonthlyBudget,
                        accountMonthlyBudget,
                        accountMonthlyBudget,
                        nextMonth,
                        false
                    )

                    addAccount(key!!, uid, account)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddAccountName.error = getString(R.string.account_name_exists)
                }

            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.add_account_error),5000)
                    .show()
            }
    }

    private fun addAccount(id: String, uid: String, account: Account) {
        databaseReference.child(id).setValue(account)
            .addOnSuccessListener {
                createCategories(uid, id)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.add_account_error),5000)
                    .show()
            }
    }

    private fun createCategories(uid: String, accountId: String) {
        val keys = mutableListOf<String>()

        val size = AppResources().getCategoryItemCount()
        for (i in 0 until size) {
            val key = databaseReference.push().key
            keys.add(key!!)
        }

        val categories = AppResources().getCategories(keys)
        databaseReference = database.getReference("categories").child(uid).child(accountId)
        databaseReference.setValue(categories)
            .addOnSuccessListener {
                hideProgressDialog()
                val action = AccountAddFragmentDirections.actionAccountAddFragmentToAccountsFragment()
                findNavController().navigate(action)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.add_categories_error),5000)
                    .show()
            }
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
        val bundle = Bundle()
        bundle.putString("title", getString(R.string.adding))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(childFragmentManager, "dialog")
    }

    private fun hideProgressDialog() {
        actionDialog.dismiss()
    }
}