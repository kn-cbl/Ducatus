package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.databinding.FragmentAccountsBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AccountsFragment : Fragment(), AccountInterface {
    private lateinit var accountAdapter: AccountAdapter
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: MaterialToolbar
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = requireActivity()
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        database = Firebase.database
        databaseReference = database.getReference("accounts").child(firebaseUser!!.uid)
        sharedPreferences = SharedPreferences(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootLayout = activity.findViewById(R.id.llAccounts)
        toolbar = activity.findViewById(R.id.tbAccounts)
        toolbar.title = getString(R.string.accounts)
        toolbar.menu.clear()

        binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.ivEditSelectedAccount.setOnClickListener {
            showPopup(it, 1)
        }

        binding.rlAddAccount.setOnClickListener {
            val action = AccountsFragmentDirections.actionAccountsFragmentToAccountAddFragment()
            findNavController().navigate(action)
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = activity.intent
        val fragment = intent.extras?.getString("setBudget")
        if (fragment == "set") {
            val accountId = intent.extras?.getString("accountId").toString()
            val action = AccountsFragmentDirections.actionAccountsFragmentToAccountEditFragment(accountId)
            findNavController().navigate(action)
        }
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun showPopup(view: View, menu: Int) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.optionSelect -> {
                    val currentAccountId = sharedPreferences.accountId.toString()
                    deselectAccount(currentAccountId, view.tag.toString())
                    true
                }
                R.id.optionEdit -> {
                    val action = AccountsFragmentDirections.actionAccountsFragmentToAccountEditFragment(view.tag.toString())
                    findNavController().navigate(action)
                    true
                }
                R.id.optionDelete -> {
                    confirmDelete(view.tag.toString())
                    true
                }
                else -> false
            }
        }

        // menu to inflate
        if (menu == 1) popup.menuInflater.inflate(R.menu.edit_options_1_menu, popup.menu)
        else if (menu == 2) popup.menuInflater.inflate(R.menu.edit_options_3_menu, popup.menu)

        popup.show()
    }

    private fun loadData() {
        showProgressDialog()
        if (firebaseUser != null) {
            val currentAccountId = sharedPreferences.accountId.toString()
            loadAccounts(currentAccountId)
            loadMainAccount(currentAccountId)

            // limit accounts to 5 per user
            if (accountAdapter.itemCount == 4) binding.rlAddAccount.visibility = View.GONE
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccounts(currentAccountId: String) {
        accountAdapter = AccountAdapter(mutableListOf(), this)
        binding.rvAccounts.adapter = accountAdapter
        binding.rvAccounts.layoutManager = LinearLayoutManager(activity)

        databaseReference.get()
            .addOnSuccessListener {
                for (child in it.children) {
                    if (child.key.toString() != currentAccountId) {
                        val account = child.getValue<Account>()
                        if (account != null) {
                            accountAdapter.addAccount(account)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, "Unable to load data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { loadAccounts(currentAccountId) }
                    .show()
            }
    }

    private fun loadMainAccount(currentAccountId: String) {
        databaseReference.child(currentAccountId).get()
            .addOnSuccessListener {
                val account = it.getValue<Account>()
                if (account != null) {
                    try {
                        val imageColor = resources.getIdentifier(
                            account.account_color.toString(),
                            "color",
                            activity.packageName
                        )

                        binding.ivSelectedAccountImage.setColorFilter(
                            ResourcesCompat.getColor(
                                resources,
                                imageColor,
                                null
                            )
                        )
                    }
                    catch (e: Exception) {}

                    val budget = "₱" + String.format("%,.2f", account.account_monthly_budget)
                    binding.tvSelectedAccountBudget.text = budget
                    binding.tvSelectedAccountName.text = account.account_name
                    binding.ivEditSelectedAccount.tag = account.account_id
                    hideProgressDialog()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, "Unable to load data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { loadMainAccount(currentAccountId) }
                    .show()
            }
    }

    private fun deselectAccount(currentAccountId: String, selectedAccountId: String) {
        showProgressDialog()
        databaseReference.child(currentAccountId).child("selected").setValue(false)
            .addOnSuccessListener {
                selectAccount(selectedAccountId)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to select account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { deselectAccount(currentAccountId, selectedAccountId) }
                    .show()
            }
    }

    private fun selectAccount(accountId: String) {
        showProgressDialog()
        databaseReference.child(accountId).get()
            .addOnSuccessListener { snapshot ->
                databaseReference.child(accountId).child("selected").setValue(true)
                    .addOnSuccessListener {
                        val account = snapshot.getValue<Account>()
                        sharedPreferences.accountId = accountId.toInt()
                        sharedPreferences.accountName = account?.account_name
                        sharedPreferences.accountColor = account?.account_color
                        loadData()
                    }
                    .addOnFailureListener {
                        hideProgressDialog()
                        Snackbar
                            .make(rootLayout, "Unable to select account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.retry)) { selectAccount(accountId) }
                            .show()
                    }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to select account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { selectAccount(accountId) }
                    .show()
            }
    }

    private fun confirmDelete(accountId: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.delete_account_mark))
            .setMessage(resources.getString(R.string.delete_account_confirm))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteAccount(accountId) }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            .show()
    }

    private fun deleteAccount(accountId: String) {
        showProgressDialog()
        databaseReference.child(accountId).removeValue()
            .addOnSuccessListener {
                loadData()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, "Unable to delete account, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { deleteAccount(accountId) }
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
        binding.pbAccount.visibility = View.VISIBLE
        binding.clAccount.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbAccount.visibility = View.INVISIBLE
        binding.clAccount.visibility = View.VISIBLE
    }
}