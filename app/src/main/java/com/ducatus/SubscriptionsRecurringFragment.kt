package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import com.ducatus.databinding.FragmentSubscriptionsRecurringBinding
import com.ducatus.interfaces.SubscriptionInterface
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class SubscriptionsRecurringFragment : Fragment(), SubscriptionInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentSubscriptionsRecurringBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private lateinit var subscriptionOverdueAdapter: SubscriptionAdapter
    private var firebaseUser: FirebaseUser? = null
    private var mutableSubscriptionsRecurring: MutableList<Subscription>? = null
    private val searchViewModel2: SearchViewModel2 by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentSubscriptionsRecurringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        searchViewModel2.searchInput.observe(viewLifecycleOwner) { name ->
            name.getContentIfNotHandled()?.let { content ->
                searchRecurringSubscriptionsByName(content.lowercase())
            }
        }

        binding.fabAddSubscription.setOnClickListener {
            startActivity(Intent(activity, SubscriptionAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun viewItem(subscriptionId: String) {
        val intent = Intent(activity, SubscriptionDetailActivity::class.java)
        intent.putExtra("subscriptionId", subscriptionId)
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadRecurringSubscriptions() }

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

    private fun loadRecurringSubscriptions() {
        showProgressDialog()
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val subscriptions = mutableListOf<Subscription>()
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 1) {
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

    private fun searchRecurringSubscriptionsByName(name: String) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("nameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                val subscriptions = mutableListOf<Subscription>()
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 1) {
                        subscriptions.add(subscription)
                    }
                }

                // search by subcategory and add to list
                searchRecurringSubscriptionsByCategory(name, subscriptions)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun searchRecurringSubscriptionsByCategory(name: String, subscriptions: MutableList<Subscription>) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("categoryNameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 1) {
                        if (!subscriptions.contains(subscription)) {
                            subscriptions.add(subscription)
                        }
                    }
                }

                // search by subcategory and add to list
                searchRecurringSubscriptionsBySubcategory(name, subscriptions)
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_subscriptions_error), 5000)
                    .show()
            }
    }

    private fun searchRecurringSubscriptionsBySubcategory(name: String, subscriptions: MutableList<Subscription>) {
        val query =
            databaseReference
                .orderByChild("subcategoryNameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val subscription = child.getValue<Subscription>()
                    if (subscription != null && subscription.frequency == 1) {
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
                        .make(rootLayout, "No recurring subscriptions found with the name $name", Snackbar.LENGTH_LONG)
                        .show()

                    if (mutableSubscriptionsRecurring?.isNotEmpty() == true) {
                        binding.tvSubscriptionsRecurringSort.visibility = View.VISIBLE
                        if (subscriptionOverdueAdapter.itemCount > 0) {
                            binding.tvSubscriptionsRecurringOverdue.visibility = View.VISIBLE
                        }

                        if (subscriptionAdapter.itemCount > 0) {
                            binding.tvSubscriptionsRecurring.visibility = View.VISIBLE
                        }
                    }
                    else {
                        binding.tvSubscriptionsRecurringSort.visibility = View.GONE
                        binding.cvSubscriptionsRecurringEmpty.visibility = View.VISIBLE
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
        binding.rvSubscriptionsRecurring.adapter = subscriptionAdapter
        binding.rvSubscriptionsRecurring.layoutManager = LinearLayoutManager(activity)

        subscriptionOverdueAdapter = SubscriptionAdapter(mutableListOf(), this)
        binding.rvSubscriptionsRecurringOverdue.adapter = subscriptionOverdueAdapter
        binding.rvSubscriptionsRecurringOverdue.layoutManager = LinearLayoutManager(activity)

        for (subscription in subscriptions) {
            // determine if subscription is overdue or not
            val zdtToday = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )
            val today = zdtToday.toInstant().toEpochMilli()
            if (today < subscription.renewsAt!!) {
                subscriptionAdapter.addSubscription(subscription)
            }
            else {
                subscriptionOverdueAdapter.addSubscription(subscription)
            }
        }

        if (subscriptionOverdueAdapter.itemCount > 0) {
            binding.tvSubscriptionsRecurringOverdue.visibility = View.VISIBLE
        }
        if (subscriptionAdapter.itemCount > 0) {
            binding.tvSubscriptionsRecurring.visibility = View.VISIBLE
        }

        if (subscriptionAdapter.itemCount <= 0 && subscriptionOverdueAdapter.itemCount <= 0) {
            mutableSubscriptionsRecurring = null
            binding.cvSubscriptionsRecurringEmpty.visibility = View.VISIBLE
        }
        else {
            mutableSubscriptionsRecurring = subscriptions
            binding.tvSubscriptionsRecurringSort.visibility = View.VISIBLE
            binding.tvSubscriptionsRecurringSort.setOnClickListener { showPopup(it) }
        }

        hideProgressDialog()
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.amountLowest -> {
                    mutableSubscriptionsRecurring?.let { subscriptions ->
                        subscriptions.sortBy { it.amount }
                        adaptSubscriptions(subscriptions)
                    }

                    true
                }
                R.id.amountHighest -> {
                    mutableSubscriptionsRecurring?.let { subscriptions ->
                        subscriptions.sortByDescending { it.amount }
                        adaptSubscriptions(subscriptions)
                    }

                    true
                }
                R.id.sortDueDateOldest -> {
                    mutableSubscriptionsRecurring?.let { subscriptions ->
                        subscriptions.sortBy { it.dueDate }
                        adaptSubscriptions(subscriptions)
                    }

                    true
                }
                R.id.sortDueDateNewest -> {
                    mutableSubscriptionsRecurring?.let { subscriptions ->
                        subscriptions.sortByDescending { it.dueDate }
                        adaptSubscriptions(subscriptions)
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
        binding.pbSubscriptionsRecurring.visibility = View.VISIBLE
        binding.cvSubscriptionsRecurringEmpty.visibility = View.GONE
        binding.tvSubscriptionsRecurringSort.visibility = View.GONE
        binding.tvSubscriptionsRecurringOverdue.visibility = View.GONE
        binding.tvSubscriptionsRecurring.visibility = View.GONE
        binding.rvSubscriptionsRecurringOverdue.visibility = View.GONE
        binding.rvSubscriptionsRecurring.visibility = View.GONE
        binding.fabAddSubscription.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbSubscriptionsRecurring.visibility = View.INVISIBLE
        binding.rvSubscriptionsRecurringOverdue.visibility = View.VISIBLE
        binding.rvSubscriptionsRecurring.visibility = View.VISIBLE
        binding.fabAddSubscription.visibility = View.VISIBLE
    }
}