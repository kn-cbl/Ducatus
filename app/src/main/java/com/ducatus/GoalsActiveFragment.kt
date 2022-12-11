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
import com.ducatus.adapter.GoalAdapter
import com.ducatus.data.Goal
import com.ducatus.databinding.FragmentGoalsActiveBinding
import com.ducatus.interfaces.GoalInterface
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
import com.google.gson.Gson

class GoalsActiveFragment : Fragment(), GoalInterface {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var binding: FragmentGoalsActiveBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var currentAccountId: String
    private lateinit var goalAdapter: GoalAdapter
    private var firebaseUser: FirebaseUser? = null
    private var mutableGoals: MutableList<Goal>? = null
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentGoalsActiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        searchViewModel.searchInput.observe(viewLifecycleOwner) { name ->
            name.getContentIfNotHandled()?.let { content ->
                searchActiveGoalByName(content.lowercase())
            }
        }

        binding.fabAddGoal.setOnClickListener {
            startActivity(Intent(activity, GoalAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseUser?.let { loadActiveGoals() }

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

    override fun viewItem(goal: Goal) {
        val intent = Intent(activity, GoalDetailActivity::class.java)
        intent.putExtra("goal", Gson().toJson(goal))
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val sharedPreferences = SharedPreferences(activity)
            currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference =
                database
                    .getReference("goals")
                    .child(firebaseUser!!.uid)
                    .child(currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadActiveGoals() {
        showProgressDialog()
        val query = databaseReference.orderByChild("status").equalTo("A")
        query.get()
            .addOnSuccessListener { snapshot ->
                val goals = mutableListOf<Goal>()
                for (child in snapshot.children) {
                    val goal = child.getValue<Goal>()
                    if (goal != null) {
                        goals.add(goal)
                    }
                }

                // sort by oldest date
                goals.sortBy { it.targetDate }
                adaptGoals(goals)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, getString(R.string.load_goals_error), 5000)
                    .show()
            }
    }

    private fun searchActiveGoalByName(name: String) {
        showProgressDialog()
        val query =
            databaseReference
                .orderByChild("nameLower")
                .startAt(name)
                .endAt(name + "\uf8ff")

        query.get()
            .addOnSuccessListener { snapshot ->
                val goals = mutableListOf<Goal>()
                for (child in snapshot.children) {
                    val goal = child.getValue<Goal>()
                    if (goal != null && goal.status == "A") {
                        goals.add(goal)
                    }
                }

                if (goals.isNotEmpty()) {
                    // sort by oldest date
                    goals.sortBy { it.targetDate }
                    adaptGoals(goals)
                }
                else {
                    hideProgressDialog()
                    Snackbar
                        .make(rootLayout, "No active goals found with the name $name", Snackbar.LENGTH_LONG)
                        .show()

                    if (mutableGoals?.isNotEmpty() == true) {
                        binding.tvGoalsActiveSort.visibility = View.VISIBLE
                        binding.rvGoalsActive.visibility = View.VISIBLE
                    }
                    else {
                        binding.tvGoalsActiveSort.visibility = View.GONE
                        binding.cvGoalsActiveEmpty.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(rootLayout, getString(R.string.load_goals_error), 5000)
                    .show()
            }
    }

    private fun adaptGoals(goals: MutableList<Goal>) {
        goalAdapter = GoalAdapter(mutableListOf(), this)
        binding.rvGoalsActive.adapter = goalAdapter
        binding.rvGoalsActive.layoutManager = LinearLayoutManager(activity)

        for (goal in goals) {
            goalAdapter.addGoal(goal)
        }

        if (goalAdapter.itemCount <= 0) {
            mutableGoals = null
            binding.cvGoalsActiveEmpty.visibility = View.VISIBLE
        }
        else {
            mutableGoals = goals
            binding.tvGoalsActiveSort.visibility = View.VISIBLE
            binding.tvGoalsActiveSort.setOnClickListener { showPopup(it) }
        }

        hideProgressDialog()
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sortTargetAmountLowest -> {
                    mutableGoals?.let { goals ->
                        goals.sortBy { it.targetAmount }
                        adaptGoals(goals)
                    }

                    true
                }
                R.id.sortTargetAmountHighest -> {
                    mutableGoals?.let { goals ->
                        goals.sortByDescending { it.targetAmount }
                        adaptGoals(goals)
                    }

                    true
                }
                R.id.sortSavedAmountLowest -> {
                    mutableGoals?.let { goals ->
                        goals.sortBy { it.savedAmount }
                        adaptGoals(goals)
                    }

                    true
                }
                R.id.sortSavedAmountHighest -> {
                    mutableGoals?.let { goals ->
                        goals.sortByDescending { it.savedAmount }
                        adaptGoals(goals)
                    }

                    true
                }
                R.id.sortTargetDateOldest -> {
                    mutableGoals?.let { goals ->
                        goals.sortBy { it.targetDate }
                        adaptGoals(goals)
                    }

                    true
                }
                R.id.sortTargetDateNewest -> {
                    mutableGoals?.let { goals ->
                        goals.sortByDescending { it.targetDate }
                        adaptGoals(goals)
                    }

                    true
                }
                else -> false
            }
        }

        // menu to inflate
        popup.menuInflater.inflate(R.menu.sort_amount_target_date, popup.menu)
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
        binding.pbGoalsActive.visibility = View.VISIBLE
        binding.cvGoalsActiveEmpty.visibility = View.GONE
        binding.tvGoalsActiveSort.visibility = View.GONE
        binding.rvGoalsActive.visibility = View.GONE
        binding.fabAddGoal.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbGoalsActive.visibility = View.GONE
        binding.fabAddGoal.visibility = View.VISIBLE
        binding.rvGoalsActive.visibility = View.VISIBLE
    }
}