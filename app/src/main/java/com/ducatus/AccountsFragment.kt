package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.AccountAdapter
import com.ducatus.data.Account
import com.ducatus.databinding.FragmentAccountsBinding
import com.ducatus.interfaces.AccountInterface
import com.ducatus.viewmodel.UpdateViewModel
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
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: MaterialToolbar
    private var firebaseUser: FirebaseUser? = null
    private val accountViewModel: UpdateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
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

        accountViewModel.isUpdated.observe(viewLifecycleOwner) { isUpdated ->
            isUpdated.getContentIfNotHandled()?.let { content ->
                if (content) loadData()
            }
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
            intent.removeExtra("setBudget")
            intent.removeExtra("accountId")

            val action = AccountsFragmentDirections.actionAccountsFragmentToAccountEditDialogFragment(accountId)
            findNavController().navigate(action)
        }
    }

    override fun showPopup(view: View, menu: Int, accountId: String) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.optionSelect -> {
                    selectAccount(accountId)
                    true
                }
                R.id.optionEdit -> {
                    val action = AccountsFragmentDirections.actionAccountsFragmentToAccountEditDialogFragment(accountId)
                    findNavController().navigate(action)
                    true
                }
                R.id.optionDelete -> {
                    confirmDelete(accountId)
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
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            database = Firebase.database
            sharedPreferences = SharedPreferences(activity)
            loadAccountsData()
        }
        else {
            sessionExpired()
        }
    }

    private fun loadAccountsData() {
        firebaseUser?.let {
            val currentAccountId = sharedPreferences.accountId.toString()
            loadAccounts(it.uid, currentAccountId)
            loadMainAccount(currentAccountId)
        }
    }

    private fun loadAccounts(uid: String, currentAccountId: String) {
        val accountAdapter = AccountAdapter(mutableListOf(), this)
        binding.rvAccounts.adapter = accountAdapter
        binding.rvAccounts.layoutManager = LinearLayoutManager(activity)

        databaseReference = database.getReference("accounts").child(uid)
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

                val text = "${accountAdapter.itemCount + 1} / 5 accounts"
                binding.tvAccountsCount.text = text

                // limit to 5 accounts per user
                if (accountAdapter.itemCount >= 4) binding.rlAddAccount.visibility = View.GONE
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_accounts_error),5000)
                    .show()
            }
    }

    private fun loadMainAccount(currentAccountId: String) {
        databaseReference.child(currentAccountId).get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()
                if (account != null) {
                    try {
                        val iconColor = resources.getIdentifier(
                            account.color.toString(),
                            "color",
                            activity.packageName
                        )

                        binding.tvSelectedAccountIcon.text = account.name?.get(0)?.uppercase()
                        binding.flSelectedAccountIcon.backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)
                    }
                    catch (e: Exception) {}

                    val budget = "₱" + String.format("%,.2f", account.monthlyBudget)
                    binding.tvSelectedAccountBudget.text = budget
                    binding.tvSelectedAccountName.text = account.name
                    binding.ivEditSelectedAccount.setOnClickListener {
                        showPopup(it, 1, account.id.toString())
                    }

                    hideProgressDialog()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_account_error),5000)
                    .show()
            }
    }

    private fun selectAccount(accountId: String) {
        showProgressDialogAction(getString(R.string.selecting))
        databaseReference.child(accountId).get()
            .addOnSuccessListener { snapshot ->
                val account = snapshot.getValue<Account>()
                account?.let {
                    sharedPreferences.accountId = it.id
                    sharedPreferences.accountName = it.name
                    sharedPreferences.accountColor = it.color

                    hideProgressDialogAction()
                    loadAccountsData()
                }
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(rootLayout, getString(R.string.select_account_error),5000)
                    .show()
            }
    }

    private fun confirmDelete(accountId: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.delete_account_title))
            .setMessage(resources.getString(R.string.delete_account_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteAccount(accountId) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteAccount(accountId: String) {
        val action = AccountsFragmentDirections.actionAccountsFragmentToDeleteAccountDialogFragment(accountId)
        findNavController().navigate(action)
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
        binding.pbAccount.visibility = View.VISIBLE
        binding.clAccount.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbAccount.visibility = View.INVISIBLE
        binding.clAccount.visibility = View.VISIBLE
    }

    private fun showProgressDialogAction(title: String) {
        val bundle = Bundle()
        bundle.putString("title", title)

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(childFragmentManager, "dialog")
    }

    private fun hideProgressDialogAction() {
        actionDialog.dismiss()
    }
}