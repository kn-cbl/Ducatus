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
import com.ducatus.adapter.SubscriptionAdapter
import com.ducatus.data.Subscription
import com.ducatus.databinding.FragmentSubscriptionsOneTimeBinding
import com.ducatus.interfaces.SubscriptionInterface
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

class SubscriptionsOneTimeFragment : Fragment(), SubscriptionInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubscriptionsOneTimeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private lateinit var subscriptionOverdueAdapter: SubscriptionAdapter
    private lateinit var subscriptionPaidAdapter: SubscriptionAdapter
    private var firebaseUser: FirebaseUser? = null
    private var mutableSubscriptionsOneTime: MutableList<Subscription>? = null
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentSubscriptionsOneTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        searchViewModel.searchInput.observe(viewLifecycleOwner) { name ->
            name.getContentIfNotHandled()?.let { content ->
                searchOneTimeSubscriptionsByName(content.lowercase())
            }
        }

        binding.fabAddSubscription.setOnClickListener {
            startActivity(Intent(activity, SubscriptionAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun viewItem(subscriptionId: String) {
        val intent = Intent(activity, SubscriptionDetailActivity::class.java)
        intent.putExtra("subscriptionId", subscriptionId)
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadOneTimeSubscriptions() }

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

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference =
                database.getReference("subscriptions")
                    .child(firebaseUser!!.uid)
                    .child(currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadOneTimeSubscriptions() {
        showProgressDialog()
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val subscriptions = mutableListOf<Subscription>()
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 0) {
                        subscriptions.add(subscription)
                    }
                }

                // sort by due date
                subscriptions.sortBy { it.dueDate!! }
                adaptSubscriptions(subscriptions)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun searchOneTimeSubscriptionsByName(name: String) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("nameLower")
                .startAt(name.lowercase())
                .endAt(name.lowercase() + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                val subscriptions = mutableListOf<Subscription>()
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 0) {
                        subscriptions.add(subscription)
                    }
                }

                // search by subcategory and add to list
                searchOneTimeSubscriptionsByCategory(name, subscriptions)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun searchOneTimeSubscriptionsByCategory(name: String, subscriptions: MutableList<Subscription>) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("categoryNameLower")
                .startAt(name.lowercase())
                .endAt(name.lowercase() + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 0) {
                        if (!subscriptions.contains(subscription)) {
                            subscriptions.add(subscription)
                        }
                    }
                }

                // search by subcategory and add to list
                searchOneTimeSubscriptionsBySubcategory(name, subscriptions)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun searchOneTimeSubscriptionsBySubcategory(name: String, subscriptions: MutableList<Subscription>) {
        val query =
            databaseReference
                .orderByChild("subcategoryNameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 0) {
                        if (!subscriptions.contains(subscription)) {
                            subscriptions.add(subscription)
                        }
                    }
                }

                if (subscriptions.isNotEmpty()) {
                    // sort by due date
                    subscriptions.sortBy { it.dueDate!! }
                    adaptSubscriptions(subscriptions)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "No one-time subscriptions found with the name $name", Snackbar.LENGTH_LONG)
                        .show()

                    if (mutableSubscriptionsOneTime?.isNotEmpty() == true) {
                        binding.tvSubscriptionsOneTimeSort.visibility = View.VISIBLE
                        if (subscriptionOverdueAdapter.itemCount > 0) {
                            binding.tvSubscriptionsOneTimeOverdue.visibility = View.VISIBLE
                        }

                        if (subscriptionAdapter.itemCount > 0) {
                            binding.tvSubscriptionsOneTime.visibility = View.VISIBLE
                        }

                        if (subscriptionPaidAdapter.itemCount > 0) {
                            binding.tvSubscriptionsOneTimePaid.visibility = View.VISIBLE
                        }
                    }
                    else {
                        binding.tvSubscriptionsOneTimeSort.visibility = View.GONE
                        binding.cvSubscriptionsOneTimeEmpty.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun adaptSubscriptions(subscriptions: MutableList<Subscription>) {
        subscriptionAdapter = SubscriptionAdapter(mutableListOf(), this)
        binding.rvSubscriptionsOneTime.adapter = subscriptionAdapter
        binding.rvSubscriptionsOneTime.layoutManager = LinearLayoutManager(activity)

        subscriptionOverdueAdapter = SubscriptionAdapter(mutableListOf(), this)
        binding.rvSubscriptionsOneTimeOverdue.adapter = subscriptionOverdueAdapter
        binding.rvSubscriptionsOneTimeOverdue.layoutManager = LinearLayoutManager(activity)

        subscriptionPaidAdapter = SubscriptionAdapter(mutableListOf(), this)
        binding.rvSubscriptionsOneTimePaid.adapter = subscriptionPaidAdapter
        binding.rvSubscriptionsOneTimePaid.layoutManager = LinearLayoutManager(activity)

        for (subscription in subscriptions) {
            // determine if subscription is overdue or not
            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )

            if (subscription.paidAt != null) {
                subscriptionPaidAdapter.addSubscription(subscription)
            }
            else if (zdt.toInstant().toEpochMilli() < subscription.dueDate!!) {
                subscriptionAdapter.addSubscription(subscription)
            }
            else {
                subscriptionOverdueAdapter.addSubscription(subscription)
            }
        }

        if (subscriptionOverdueAdapter.itemCount > 0) {
            binding.tvSubscriptionsOneTimeOverdue.visibility = View.VISIBLE
        }
        if (subscriptionAdapter.itemCount > 0) {
            binding.tvSubscriptionsOneTime.visibility = View.VISIBLE
        }
        if (subscriptionPaidAdapter.itemCount > 0) {
            binding.tvSubscriptionsOneTimePaid.visibility = View.VISIBLE
        }

        if (subscriptionAdapter.itemCount <= 0 && subscriptionOverdueAdapter.itemCount <= 0 && subscriptionPaidAdapter.itemCount <= 0) {
            mutableSubscriptionsOneTime = null
            binding.cvSubscriptionsOneTimeEmpty.visibility = View.VISIBLE
        }
        else {
            mutableSubscriptionsOneTime = subscriptions
            binding.tvSubscriptionsOneTimeSort.visibility = View.VISIBLE
            binding.tvSubscriptionsOneTimeSort.setOnClickListener { showPopup(it) }
        }

        hideProgressDialog()
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sortNameUp -> {
                    mutableSubscriptionsOneTime?.let { list ->
                        list.sortBy { it.nameLower }
                        adaptSubscriptions(list)
                    }

                    true
                }
                R.id.sortNameDown -> {
                    mutableSubscriptionsOneTime?.let { list ->
                        list.sortByDescending { it.nameLower }
                        adaptSubscriptions(list)
                    }

                    true
                }
                R.id.sortCategory -> {
                    mutableSubscriptionsOneTime?.let { list ->
                        list.sortBy { it.categoryNameLower }
                        adaptSubscriptions(list)
                    }

                    true
                }
                R.id.sortAmountLowest -> {
                    mutableSubscriptionsOneTime?.let { list ->
                        list.sortBy { it.amount }
                        adaptSubscriptions(list)
                    }

                    true
                }
                R.id.sortAmountHighest -> {
                    mutableSubscriptionsOneTime?.let { list ->
                        list.sortByDescending { it.amount }
                        adaptSubscriptions(list)
                    }

                    true
                }
                R.id.sortDueDateOldest -> {
                    mutableSubscriptionsOneTime?.let { list ->
                        list.sortBy { it.dueDate }
                        adaptSubscriptions(list)
                    }

                    true
                }
                R.id.sortDueDateNewest -> {
                    mutableSubscriptionsOneTime?.let { list ->
                        list.sortByDescending { it.dueDate }
                        adaptSubscriptions(list)
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
        binding.pbSubscriptionsOneTime.visibility = View.VISIBLE
        binding.tvSubscriptionsOneTimeSort.visibility = View.GONE
        binding.cvSubscriptionsOneTimeEmpty.visibility = View.GONE
        binding.tvSubscriptionsOneTimeOverdue.visibility = View.GONE
        binding.tvSubscriptionsOneTime.visibility = View.GONE
        binding.tvSubscriptionsOneTimePaid.visibility = View.GONE
        binding.rvSubscriptionsOneTimeOverdue.visibility = View.GONE
        binding.rvSubscriptionsOneTime.visibility = View.GONE
        binding.rvSubscriptionsOneTimePaid.visibility = View.GONE
        binding.fabAddSubscription.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbSubscriptionsOneTime.visibility = View.INVISIBLE
        binding.rvSubscriptionsOneTimeOverdue.visibility = View.VISIBLE
        binding.rvSubscriptionsOneTime.visibility = View.VISIBLE
        binding.rvSubscriptionsOneTimePaid.visibility = View.VISIBLE
        binding.fabAddSubscription.visibility = View.VISIBLE
    }
}