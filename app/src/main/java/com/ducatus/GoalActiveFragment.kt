package com.ducatus

import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.GoalAdapter
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.ActiveGoalIntf
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.GoalDetailIntf
import com.ducatus.interfaces.GoalIntf
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
    private lateinit var parentView: View
    lateinit var pbLoading: ProgressBar
    lateinit var txtNoActive: TextView


    val goalIntf = object : GoalIntf {
        override fun PressBack() {
            val callback = object : FirebaseDatabaseCallback {
                override fun onSuccessInsert(key: String) {
                    TODO("Not yet implemented")
                }

                override fun onError(e: Exception) {
                    Log.e("ERROR_ACTIVE_FRAGMENT", e.message.toString())
                }

                override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                    TODO("Not yet implemented")
                }

                override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                    val newList = mutableListOf<Goals>()
                    for (l in goalsList) {
                        if (l.status == 0) {
                            newList.add(l)
                        }
                    }
                    var adapterCallback = object : ActiveGoalIntf {
                        override fun OnClickListener(mView: View, position: Int) {
                            GoalDetail.start(
                                requireContext(),
                                goalsList.get(position),
                                object : GoalDetailIntf {
                                    override fun deleteSubmitted() {
                                        recycler.adapter = null
                                        loadData(parentView)
                                    }

                                    override fun onSuccessUpdate() {
                                        recycler.adapter = null
                                        loadData(parentView)
                                    }
                                }, false
                            )
                        }
                    }
                    listOfGoals = newList
                    adapter = GoalAdapter(requireContext(), newList, adapterCallback)
                    recycler.layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    recycler.adapter = adapter
                }
            }
            db.getAllDataFromDB("Goals", accountID, callback)
        }

        override fun OnToolbarClickListener(mView: View) {
            TODO("Not yet implemented")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var mView: View =
            inflater.inflate(R.layout.fragment_goal_active_container, container, false)
        initViews(mView)
        initListeners(mView)
        mView.setOnFocusChangeListener(View.OnFocusChangeListener { v, hasFocus ->
            loadData(mView)
        })
        return mView
    }

    private fun initListeners(mView: View) {
        btnAdd.setOnClickListener(View.OnClickListener {
            btnAdd.isEnabled = false
            NewGoal.start(requireContext(), goalIntf)
        })
    }

    private fun initViews(mView: View) {
        val sharedPreferences = SharedPreferences(mView.context)
        accountID = sharedPreferences.accountId!!
        recycler = mView.findViewById(R.id.recycler)
        btnAdd = mView.findViewById(R.id.fab_goalActive)
        pbLoading = mView.findViewById(R.id.pbLoading)
        txtNoActive = mView.findViewById(R.id.txtNoActive)
        txtNoActive.visibility = View.GONE
        parentView = mView
        loadData(mView)

    }

    private fun loadData(mView: View) {
        val callback: FirebaseDatabaseCallback = object : FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                createGoalTemp(mView, goalsList)
            }

            override fun onError(e: Exception) {
                Log.e("GOALS_ERROR", e.message.toString())
                pbLoading.visibility = View.GONE
                txtNoActive.visibility = View.VISIBLE
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                TODO("Not yet implemented")
            }
        }
        db.getAllDataFromDB("Goals", accountID, callback)
    }

    private fun createGoalTemp(mView: View, list: List<Goals>) {
        val newList = mutableListOf<Goals>()
        for (l in list) {
            if (l.status == 0) {
                newList.add(l)
            }
        }
        if (newList.size == 0) {
            pbLoading.visibility = View.GONE
            txtNoActive.visibility = View.VISIBLE
            return;
        } else {
            pbLoading.visibility = View.GONE
            txtNoActive.visibility = View.GONE
        }
        var adapterCallback = object : ActiveGoalIntf {
            override fun OnClickListener(mView: View, position: Int) {
                GoalDetail.start(requireContext(), list.get(position), object : GoalDetailIntf {
                    override fun deleteSubmitted() {
                        recycler.adapter = null
                        loadData(parentView)
                    }

                    override fun onSuccessUpdate() {
                        recycler.adapter = null
                        loadData(parentView)
                    }
                }, false)
            }
        }
        adapter = GoalAdapter(requireContext(), newList, adapterCallback)
        recycler.layoutManager =
            LinearLayoutManager(mView.context, LinearLayoutManager.VERTICAL, false)
        recycler.adapter = adapter

    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        txtNoActive.visibility = View.GONE
        pbLoading.visibility = View.VISIBLE
        btnAdd.isEnabled = true
        recycler.adapter = null
        loadData(parentView)
    }


}