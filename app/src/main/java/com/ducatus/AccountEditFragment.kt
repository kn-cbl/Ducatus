package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ducatus.databinding.FragmentAccountEditBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AccountEditFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountEditBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private var currentName: String? = null
    private var currentBudget: Double? = null
    private var currentColor: String? = null
    private var colors: List<String> = listOf()
    private val args: AccountEditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        binding = FragmentAccountEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        inputObserver()

        binding.spEditColor.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedView = binding.spEditColor.selectedView
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

        binding.btnEditAccountCancel.setOnClickListener {
            dismiss()
        }

        binding.btnEditAccountSave.setOnClickListener {
            validateData()
        }
    }

    private fun inputObserver() {
        binding.tfEditAccountName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditAccountName.error = getString(R.string.account_name_empty)
            else binding.tfEditAccountName.error = null
        }
        binding.tfEditAccountBudget.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
            else binding.tfEditAccountBudget.error = null
        }
        binding.tfEditAccountBudget.editText?.doAfterTextChanged { text ->
            if (text.toString().startsWith("0")) text?.clear()
        }
    }

    private fun loadData() {
        showLoadingData()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            loadColors()

            database = Firebase.database
            databaseReference = database.getReference("accounts").child(firebaseUser.uid).child(args.accountId)
            databaseReference.get()
                .addOnSuccessListener {
                    val account = it.getValue<Account>()
                    if (account != null) {
                        currentName = account.account_name.toString()
                        currentBudget = account.account_monthly_budget
                        currentColor = account.account_color

                        binding.tfEditAccountName.editText?.setText(currentName)
                        binding.tfEditAccountBudget.editText?.setText(currentBudget?.toInt().toString())

                        val index = colors.indexOf(account.account_color)
                        binding.spEditColor.setSelection(index)

                        hideLoadingData()
                    }
                }
                .addOnFailureListener {
                    Snackbar
                        .make(rootLayout, "Unable to load data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { loadData() }
                        .show()
                }
        }
        else {
            sessionExpired()
        }
    }

    private fun loadColors() {
        colors = AppResources().getColors()
        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.spinner_item, R.id.txt_bundle, colors) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = getView(position, convertView, parent)
                val itemView = view.findViewById<View>(R.id.viewHelperItem)
                val gradientDrawable: GradientDrawable = itemView.background as GradientDrawable

                val iconColor = resources.getIdentifier(
                    colors[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                itemView.background = gradientDrawable
                return view
            }
        }

        binding.spEditColor.adapter = adapter
    }

    private fun validateData() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val accountName = binding.tfEditAccountName.editText?.text.toString().trim {it <= ' '}
        val accountMonthlyBudget = binding.tfEditAccountBudget.editText?.text.toString().trim {it <= ' '}
        val accountColor = binding.spEditColor.selectedItem.toString()

        if (accountName == currentName && accountMonthlyBudget.toDouble() == currentBudget && accountColor == currentColor) {
            activity.onBackPressed()
        }
        else {
            if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountMonthlyBudget) || accountMonthlyBudget.toDouble() < 1) {
                if (TextUtils.isEmpty(accountName)) binding.tfEditAccountName.error = getString(R.string.account_name_empty)
                if (TextUtils.isEmpty(accountMonthlyBudget)) binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
                if (accountMonthlyBudget.startsWith("0")) binding.tfEditAccountBudget.error = getString(R.string.budget_amount_0)
            }
            else {
                updateAccount(args.accountId.toInt(), accountName, accountMonthlyBudget.toDouble(), accountColor)
            }
        }
    }

    private fun updateAccount(accountId: Int, accountName: String, accountMonthlyBudget: Double, accountColor: String) {
        showProgressDialog()
        val account = Account(accountId, accountName, accountColor, accountMonthlyBudget, accountMonthlyBudget)
        databaseReference.setValue(account)
            .addOnSuccessListener {
                hideProgressDialog()
                val action = AccountEditFragmentDirections.actionAccountEditFragmentToAccountsFragment()
                findNavController().navigate(action)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to update account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { updateAccount(accountId, accountName, accountMonthlyBudget, accountColor) }
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

    private fun showLoadingData() {
        binding.pbEditAccountLoad.visibility = View.VISIBLE
        binding.llEditAccount.visibility = View.GONE
    }

    private fun hideLoadingData() {
        binding.pbEditAccountLoad.visibility = View.INVISIBLE
        binding.llEditAccount.visibility = View.VISIBLE
    }

    private fun showProgressDialog() {
        binding.pbEditAccount.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.pbEditAccount.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}