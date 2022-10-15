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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentAccountAddBinding
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
    private var colors: List<String> = listOf()

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
        loadColors()
        inputObserver()

        binding.spAddAccountColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spAddAccountColor.selectedView
                val color = selectedView.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable = GradientDrawable()

                val iconColor = resources.getIdentifier(
                    colors[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                color.background = gradientDrawable
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.done -> {
                    validateData()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadColors() {
        colors = AppResources().getColors()
        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.spinner_item, R.id.txt_bundle, colors) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = getView(position, convertView, parent)
                val color = view.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable: GradientDrawable = color.background as GradientDrawable

                val iconColor = resources.getIdentifier(
                    colors[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                color.background = gradientDrawable
                return view
            }
        }

        binding.spAddAccountColor.adapter = adapter
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
        val accountColor = binding.spAddAccountColor.selectedItem.toString()

        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountMonthlyBudget) || accountMonthlyBudget.toDouble() < 1) {
            if (TextUtils.isEmpty(accountName)) binding.tfAddAccountName.error = getString(R.string.account_name_empty)
            if (TextUtils.isEmpty(accountMonthlyBudget)) binding.tfAddAccountBudget.error = getString(R.string.monthly_budget_empty)
            if (accountMonthlyBudget.startsWith("0")) binding.tfAddAccountBudget.error = getString(R.string.budget_amount_0)
        }
        else {
            auth = Firebase.auth
            val firebaseUser: FirebaseUser? = auth.currentUser
            if (firebaseUser != null) {
                accountExists(firebaseUser.uid, accountName, accountMonthlyBudget.toDouble(), accountColor)
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
        databaseReference = database.getReference("categories").child(uid).child(accountId)

        val categories = mapOf(
            "0" to Category(
                0, "Electronics", 1,
                "blue", "ic_baseline_devices_24"),

            "1" to Category(
                1, "Financial Expenses", 0,
                "material_dark_yellow_a400", "ic_baseline_wallet_24"),

            "2" to Category(
                2, "Food and Drinks", 0,
                "material_bright_red_a400", "ic_baseline_fastfood_24"),

            "3" to Category(
                3, "Housing", 0,
                "material_orange_a400", "ic_baseline_home_24"),

            "4" to Category(
                4, "Investments", 0,
                "dark_green", "ic_local_investment_24"),

            "5" to Category(
                5, "Life and Entertainment", 1,
                "material_cyan_a400", "ic_baseline_videogame_asset_24"),

            "6" to Category(
                6, "Shopping", 1,
                "dark_pink", "ic_outline_shopping_bag_24"),

            "7" to Category(
                7, "Transportation", 0,
                "dark_brown", "ic_baseline_directions_bus_24"),

            "8" to Category(
                8, "Vehicle", 1,
                "material_dark_purple_a400", "ic_baseline_directions_car_24"),

            "9" to Category(
                9, "Others", 1,
                "light_gray", "ic_baseline_more_horiz_24"),
        )

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
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
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