package com.ducatus

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ducatus.databinding.FragmentAccountsEditBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AccountsEditFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountsEditBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var currentName: String
    private var currentBudget: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llAccounts)
        binding = FragmentAccountsEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadData()

        binding.btnEditAccount.setOnClickListener {
            validateChanges()
        }
    }

    private fun loadData() {
        showLoadingData()

        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            databaseReference = database.getReference("accounts/" + firebaseUser.uid + "/0")
            databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val account = snapshot.getValue(Account::class.java)
                    if (account != null) {
                        currentName = account.account_name.toString()
                        currentBudget = account.account_monthly_budget

                        val imageColor = resources.getIdentifier(account.account_color.toString(), "color", activity.packageName)
//                        binding.ivSelectedAccountImage.setColorFilter(ResourcesCompat.getColor(resources, imageColor, null))
                        binding.tfEditAccountName.editText?.setText(currentName)
                        binding.tfEditAccountBudget.editText?.setText(currentBudget.toString())

                        hideLoadingData()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar
                        .make(rootLayout, "Failed to load data", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") { loadData() }
                        .show()
                }
            })
        }
        else {
            sessionExpired()
        }
    }

    private fun validateChanges() {
        // hide keyboard
        try {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }
        catch (e: Exception){}

        val accountName = binding.tfEditAccountName.editText?.text.toString().trim {it <= ' '}
        val monthlyBudget = binding.tfEditAccountBudget.editText?.text.toString().trim {it <= ' '}

        if (accountName == currentName && monthlyBudget.toDouble() == currentBudget) {
            activity.onBackPressed()
        }
        else {
            if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(monthlyBudget)) {
                if (TextUtils.isEmpty(accountName)) binding.tfEditAccountName.error = getString(R.string.account_name_empty)
                if (TextUtils.isEmpty(monthlyBudget)) binding.tfEditAccountBudget.error = getString(R.string.monthly_budget_empty)
            }
            else {
                updateAccount(accountName, monthlyBudget.toDouble())
            }
        }
    }

    private fun updateAccount(accountName: String, monthlyBudget: Double) {
        showProgressDialog()

        val account = Account(0, accountName, monthlyBudget, "green_primary")
        databaseReference.setValue(account)
            .addOnSuccessListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Successfully updated account", Snackbar.LENGTH_LONG)
                    .show()

                // add 3 second delay
                object : CountDownTimer(3000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // do nothing
                    }
                    override fun onFinish() {
                        activity.onBackPressed()
                    }
                }.start()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Failed to update account", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {updateAccount(accountName, monthlyBudget)}
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        activity.finish()
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
        binding.btnEditAccount.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.light_gray_text)
        binding.pbEditAccount.visibility = View.VISIBLE
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun hideProgressDialog() {
        binding.btnEditAccount.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.green_primary)
        binding.pbEditAccount.visibility = View.INVISIBLE
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}