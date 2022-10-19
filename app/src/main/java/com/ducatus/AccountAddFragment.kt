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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentAccountAddBinding
import com.ducatus.viewmodel.ColorViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AccountAddFragment : Fragment() {
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

        colorViewModel.selectedColor.observe(viewLifecycleOwner) { selectedColor ->
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
            binding.tfAddAccountBudget.error = getString(R.string.budget_amount_0)
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
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                var nameKey = false

                for (child in snapshot.children) {
                    if (accountName == child.child("account_name").value.toString()) {
                        nameKey = true
                        break
                    }
                }

                if (!nameKey) {
                    val lastId = snapshot.childrenCount.toInt()
                    val account = Account(lastId, accountName, accountColor, accountMonthlyBudget, accountMonthlyBudget)
                    addAccount(lastId.toString(), uid, account)
                }
                else {
                    hideProgressDialog()
                    binding.tfAddAccountName.error = getString(R.string.account_name_exists)
                }

            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { accountExists(uid, accountName, accountMonthlyBudget, accountColor) }
                    .show()
            }
    }

    private fun addAccount(id: String, uid: String, account: Account) {
        showProgressDialog()
        databaseReference.child(id).setValue(account)
            .addOnSuccessListener {
                createCategories(uid, id)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to add account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { addAccount(id, uid, account) }
                    .show()
            }
    }

    private fun createCategories(uid: String, accountId: String) {
        showProgressDialog()
        val categories = AppResources().getDefaultCategories()

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
                    .make(rootLayout, "Unable to add account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { createCategories(uid, accountId) }
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
        binding.pbAddAccount.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbAddAccount.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}