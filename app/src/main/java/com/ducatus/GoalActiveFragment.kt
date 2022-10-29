package com.ducatus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.GoalAdapter
import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.services.LocalFirebaseDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.Exception


class GoalActiveFragment() : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnAdd: FloatingActionButton
    private var listOfGoals: List<Goals> = ArrayList<Goals>()
    private lateinit var accountID: String
    private lateinit var adapter: GoalAdapter
    private val db: LocalFirebaseDatabase = LocalFirebaseDatabase()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var mView: View =
            inflater.inflate(R.layout.fragment_goal_active_container, container, false)
        initViews(mView)
        initListeners(mView)
        return mView
    }

    private fun initListeners(mView: View) {
        btnAdd.setOnClickListener(View.OnClickListener {
            btnAdd.isEnabled = false
            val intent: Intent = Intent(mView.context, NewGoal::class.java)
            startActivity(intent)
        })
    }

    private fun initViews(mView: View) {
        val sharedPreferences = SharedPreferences(mView.context)
        accountID = sharedPreferences.accountId!!
        recycler = mView.findViewById(R.id.recycler)
        btnAdd = mView.findViewById(R.id.fab_goalActive)
        loadData(mView)
        createGoalTemp(mView)
    }

    private fun loadData(mView: View) {
        val callback: FirebaseDatabaseCallback = object : FirebaseDatabaseCallback {
            override fun onSuccessInsert() {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                listOfGoals = goalsList
                createGoalTemp(mView)
            }

            override fun onError(e: Exception) {
                Log.e("GOALS_ERROR", e.message.toString())
                Toast.makeText(mView.context, "Internal System Error", Toast.LENGTH_SHORT).show()
            }
        }
        db.getAllDataFromDB("Goals", accountID, callback)
    }

    private fun createGoalTemp(mView: View) {
        adapter = GoalAdapter(mView.context, listOfGoals)
        recycler.layoutManager =
            LinearLayoutManager(mView.context, LinearLayoutManager.VERTICAL, false)
        recycler.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        btnAdd.isEnabled = true
    }

}