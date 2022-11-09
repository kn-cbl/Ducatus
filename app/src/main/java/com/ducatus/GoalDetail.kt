package com.ducatus

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.GoalHistoryAdapter
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.GoalDetailIntf
import com.ducatus.services.LocalFirebaseDatabase
import kotlinx.android.synthetic.main.goal_detail.*
import java.lang.Exception
import java.time.LocalDate

class GoalDetail : AppCompatActivity() {

    lateinit var goals: Goals
    lateinit var txtPercentage: TextView
    lateinit var txtSavedAmount: TextView
    lateinit var txtGoalRemainingAmount: TextView
    lateinit var txtGoalDescription: TextView
    lateinit var imgGoal: ImageView
    lateinit var txtGoalAmount: TextView
    lateinit var txtEarnedAmount: TextView
    lateinit var btnSave: Button
    lateinit var btnSetReached: Button
    lateinit var pb: ProgressBar
    lateinit var txtTargetDate: TextView
    lateinit var gDetailIntf: GoalDetailIntf
    var isFromPause: Boolean = false
    var isFromReach: Boolean = false
    lateinit var alertDiag: AlertDialog
    lateinit var editDialogAmount: EditText
    lateinit var btnDiagSave: Button
    lateinit var db: LocalFirebaseDatabase
    lateinit var progressDiag: ProgressDialog
    lateinit var recycler: RecyclerView
    lateinit var goalHistoryAdapter: GoalHistoryAdapter
    lateinit var ghList: List<GoalHistory>

    companion object {
        lateinit var goal: Goals
        lateinit var goalDetailIntf: GoalDetailIntf
        var isFromPause: Boolean = false

        fun start(
            mContext: Context,
            g: Goals,
            detailIntf: GoalDetailIntf,
            flag: Boolean
        ) {
            goal = g
            goalDetailIntf = detailIntf
            isFromPause = flag
            val intent: Intent = Intent(mContext, GoalDetail::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.goal_detail)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
        initViews();
    }

    private fun initViews() {
        goals = GoalDetail.goal
        gDetailIntf = GoalDetail.goalDetailIntf
        isFromPause = GoalDetail.isFromPause
        ghList = mutableListOf<GoalHistory>()
        db = LocalFirebaseDatabase()
        progressDiag = ProgressDialog(this)
        progressDiag.setMessage("Sending Request ...")
        progressDiag.setCancelable(false)
        txtPercentage = findViewById(R.id.textView_goalPercent)
        txtSavedAmount = findViewById(R.id.textView_savedAmount)
        txtGoalAmount = findViewById(R.id.textView_goalGoalAmount)
        txtGoalRemainingAmount = findViewById(R.id.textView_goalRemainingAmount)
        txtEarnedAmount = findViewById(R.id.textView_goalEarnedAmount)
        btnSave = findViewById(R.id.btn_savedGoalAmount)
        btnSetReached = findViewById(R.id.btn_setGoalReached)
        pb = findViewById(R.id.goalDetail_progressBar)
        txtTargetDate = findViewById(R.id.targetDate_goalDetail)
        txtGoalDescription = findViewById(R.id.textView_goalDetail)
        imgGoal = findViewById(R.id.img_goalDetail)
        recycler = findViewById(R.id.savedAmountHistoryListView)

        btnSetReached.isEnabled = goals.remaining == 0.0

        txtPercentage.setText(String.format("%.2f", goals.percentage) + "%")
        if (goals.earned == 0.0) {
            pb.setProgress(0)
        } else {
            pb.setProgress(goals.percentage.toInt())
        }



        txtGoalAmount.setText(String.format("P%.2f", goals.goalAmount))
        txtGoalRemainingAmount.setText(String.format("P%.2f", goals.remaining))
        txtEarnedAmount.setText(String.format("P%.2f", goals.earned))
        txtTargetDate.setText("Target Date: " + goals.targetDate)
        txtGoalDescription.setText(goals.goalDescription)
        imgGoal.setBackgroundColor(goals.color)
        imgGoal.setImageResource(goals.icon)
        txtSavedAmount.setText(String.format("P%.2f", goals.earned))

        if (isFromPause) {
            btnSave.isEnabled = false
            btnSetReached.isEnabled = false
        } else {
            btnSave.isEnabled = true
            btnSetReached.isEnabled = true
        }

        initListeners();
        loadHistory();
    }

    private fun loadHistory() {
        db.getAllDataFromDB("Goal History", goal.accountID, object : FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                var newHistory = mutableListOf<GoalHistory>()
                for (g in goalHistoryList) {
                    if (g.goalkey.equals(goal.key)) {
                        newHistory.add(g)
                    }
                }
                ghList = newHistory
                goalHistoryAdapter = GoalHistoryAdapter(applicationContext, newHistory)
                recycler.layoutManager =
                    GridLayoutManager(applicationContext, 2, GridLayoutManager.VERTICAL, false)
                recycler.adapter = goalHistoryAdapter
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                TODO("Not yet implemented")
            }

            override fun onError(e: Exception) {
                Log.e("ERROR_LOADING_HISTORY", e.message.toString())
            }
        })
    }

    private fun initListeners() {
        goalDetail_toolbar.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })
        goalDetail_toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit -> {
                    EditGoal.start(
                        this, goals,
                        object : GoalDetailIntf {
                            override fun deleteSubmitted() {
                                gDetailIntf.deleteSubmitted()
                                finish()
                            }

                            override fun onSuccessUpdate() {
                                gDetailIntf.onSuccessUpdate()
                                finish()
                            }
                        },
                        isFromPause, false, ghList
                    )
                    true
                }
                else -> {
                    false
                }
            }
        })
        btnSave.setOnClickListener(View.OnClickListener {
            val mBuilder = AlertDialog.Builder(this)
            val mView = LayoutInflater.from(this).inflate(R.layout.dialog_input_amount, null, false)
            initDialogViews(mView)
            initDiagListeners()
            mBuilder.setView(mView)
            alertDiag = mBuilder.create()
            alertDiag.show()
        })
        btnSetReached.setOnClickListener(View.OnClickListener {
            if (goals.remaining == 0.0 && goals.earned == goals.goalAmount) {
                val iBuilder = AlertDialog.Builder(this)
                val dialogListener = object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        when (which) {
                            DialogInterface.BUTTON_NEGATIVE -> {
                                progressDiag.show()
                                var newGoal = goals
                                var entities = LocalEntities()
                                entities.goals = newGoal
                                db.updateToDb(
                                    entities,
                                    "Goal Reached",
                                    object : FirebaseDatabaseCallback {
                                        override fun onSuccessInsert(key: String) {
                                            progressDiag.dismiss()
                                            Toast.makeText(
                                                applicationContext,
                                                "Successfully Saved Amount Info",
                                                Toast.LENGTH_SHORT
                                            )
                                            finish()
                                        }

                                        override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                                            TODO("Not yet implemented")
                                        }

                                        override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                                            TODO("Not yet implemented")
                                        }

                                        override fun onError(e: Exception) {
                                            progressDiag.dismiss()
                                            Log.e("ERROR_UPDATE", e.message.toString())
                                        }
                                    })
                            }
                        }
                    }
                }
                iBuilder.setMessage("You are about to set this goal as reached.\nBy clicking yes, You certified that the Goal Amount: P" + goals.goalAmount.toString() + " has been satisfied")
                    .setNegativeButton("Yes", dialogListener)
                    .setPositiveButton("No", dialogListener)
                    .show()
            } else {
                Toast.makeText(
                    applicationContext,
                    "You need to have at least 100% of the goal to set this to reach",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun initDiagListeners() {
        btnDiagSave.setOnClickListener(View.OnClickListener {
            if (editDialogAmount.text.toString().equals("")) {
                Toast.makeText(
                    applicationContext,
                    "Please don't leave empty fields",
                    Toast.LENGTH_SHORT
                ).show()
            } else {

                if (editDialogAmount.text.toString().toDouble() > goals.goalAmount) {
                    Toast.makeText(
                        applicationContext,
                        "Amount exceed the target goal amount, Please change it",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }
                if (editDialogAmount.text.toString().toDouble() > goals.remaining) {
                    Toast.makeText(
                        applicationContext,
                        String.format(
                            "Amount exceed the remaining amount P%.2f, Please change it",
                            goals.remaining
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }
                if (editDialogAmount.text.toString().toDouble() <= 0) {
                    Toast.makeText(
                        applicationContext,
                        "Amount must not be less than or equal to zero",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }

                progressDiag.show()
                var goalHistory = GoalHistory()
                goalHistory.goalkey = goal.key
                goalHistory.accountID = goal.accountID
                goalHistory.amountPaid = editDialogAmount.text.toString().toDouble()
                goalHistory.datePaid = LocalDate.now().toString()

                var entities = LocalEntities()
                entities.goalHistory = goalHistory
                var newGoal = goals
                newGoal.earned += goalHistory.amountPaid
                newGoal.remaining = newGoal.goalAmount - newGoal.earned
                newGoal.percentage = newGoal.earned / newGoal.goalAmount * 100
                entities.goals = newGoal

                var entityName = "Goals"
//                if ((newGoal.earned == newGoal.goalAmount) && (newGoal.remaining == 0.0)) {
//                    entityName = "Goal Reached"
//                }

                db.updateToDb(entities, entityName, object : FirebaseDatabaseCallback {
                    override fun onSuccessInsert(key: String) {
                        saveGoalHistory(entities)
                    }

                    override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                        TODO("Not yet implemented")
                    }

                    override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                        TODO("Not yet implemented")
                    }

                    override fun onError(e: Exception) {
                        Log.e("ERROR_SAVING_GOALS_FOR_HISTORY", e.message.toString())
                    }
                });

            }
        })
    }

    private fun saveGoalHistory(entities: LocalEntities) {
        db.writeToDb(entities, "Goal History", object : FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                progressDiag.dismiss()
                Toast.makeText(
                    applicationContext,
                    "Successfully Saved Amount Info",
                    Toast.LENGTH_SHORT
                )
                finish()
            }

            override fun onError(e: Exception) {
                progressDiag.dismiss()
                Log.e("ERROR_GOAL_DETAIL", e.message.toString())
                Toast.makeText(
                    applicationContext,
                    "Failed to Save Amount Info",
                    Toast.LENGTH_SHORT
                )
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun initDialogViews(mView: View) {
        editDialogAmount = mView.findViewById(R.id.editAmount)
        btnDiagSave = mView.findViewById(R.id.btnSave)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }
}