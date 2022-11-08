package com.ducatus.services

import android.util.Log
import com.ducatus.common.Common
import com.ducatus.common.Constants
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import java.time.LocalDate
import java.util.*

class LocalFirebaseDatabase {

    private val db: FirebaseDatabase = FirebaseDatabase.getInstance();

    constructor()

    fun updateToDb(
        entities: LocalEntities,
        entityName: String,
        listener: FirebaseDatabaseCallback
    ) {
        when (entityName) {
            "Goals" -> {
                val goals: Goals = entities.goals
                goals.status = Constants().GOAL_IN_PROGRESS
                val finalMap = Common().toGoalsMap(goals)
                db.getReference("goals")
                    .child(goals.accountID)
                    .child(goals.key)
                    .setValue(finalMap)
                    .addOnSuccessListener {
                        listener.onSuccessInsert(goals.key)
                    }
                    .addOnFailureListener(OnFailureListener { e ->
                        listener.onError(e)
                    })
            }
            "Goals Pause" -> {
                val goals: Goals = entities.goals
                goals.status = Constants().GOAL_PAUSE
                goals.dateGoalPaused = LocalDate.now().toString()
                val finalMap = Common().toGoalsMap(goals)
                db.getReference("goals")
                    .child(goals.accountID)
                    .child(goals.key)
                    .setValue(finalMap)
                    .addOnSuccessListener {
                        listener.onSuccessInsert(goals.key)
                    }
                    .addOnFailureListener(OnFailureListener { e ->
                        listener.onError(e)
                    })
            }
            "Goal Reached" -> {
                val goals: Goals = entities.goals
                goals.status = Constants().GOAL_REACHED
                val finalMap = Common().toGoalsMap(goals)
                db.getReference("goals")
                    .child(goals.accountID)
                    .child(goals.key)
                    .setValue(finalMap)
                    .addOnSuccessListener {
                        listener.onSuccessInsert(goals.key)
                    }
                    .addOnFailureListener(OnFailureListener { e ->
                        listener.onError(e)
                    })
            }
            "Goal History" -> {
                val goalHistory: GoalHistory = entities.goalHistory
                val finalMap = Common().toGoalHistory(goalHistory)
                db.getReference("goalHistory")
                    .child(goalHistory.accountID)
                    .child(goalHistory.goalkey)
                    .child(goalHistory.goalHistoryKey)
                    .setValue(finalMap)
                    .addOnSuccessListener(OnSuccessListener {
                        listener.onSuccessInsert(goalHistory.goalkey)
                    })
                    .addOnFailureListener(OnFailureListener { e ->
                        listener.onError(e)
                    })
            }
        }
    }

    fun writeToDb(entities: LocalEntities, entityName: String, listener: FirebaseDatabaseCallback) {
        when (entityName) {
            "Goals" -> {
                val goals: Goals = entities.goals

                val key = db.getReference("goals")
                    .child(goals.accountID).push().key!!
                goals.key = key
                goals.status = Constants().GOAL_IN_PROGRESS
                val finalMap = Common().toGoalsMap(goals)
                db.getReference("goals")
                    .child(goals.accountID)
                    .child(key)
                    .setValue(finalMap)
                    .addOnSuccessListener {
                        listener.onSuccessInsert(key)
                    }
                    .addOnFailureListener(OnFailureListener { e ->
                        listener.onError(e)
                    })
            }
            "Goal History" -> {
                val goalHistory: GoalHistory = entities.goalHistory
                val key = db.getReference("goalHistory")
                    .child(goalHistory.accountID).push().key!!
                goalHistory.goalHistoryKey = key
                val finalMap = Common().toGoalHistory(goalHistory)
                db.getReference("goalHistory")
                    .child(goalHistory.accountID)
                    .child(key)
                    .setValue(finalMap)
                    .addOnSuccessListener(OnSuccessListener {
                        listener.onSuccessInsert(goalHistory.goalkey)
                    })
                    .addOnFailureListener(OnFailureListener { e ->
                        listener.onError(e)
                    })
            }
        }
    }

    fun getAllDataFromDB(
        entityName: String,
        accountID: String,
        listener: FirebaseDatabaseCallback
    ) {
        when (entityName) {
            "Goals" -> {
                try {
                    db.getReference("goals")
                        .child(accountID)
                        .get()
                        .addOnSuccessListener { dataSnapShot ->
                            try {
                                var hash = dataSnapShot.getValue<Map<String, Object>>()!!
                                Log.e("HASH", hash.toString())
                                listener.onSuccessListOfGoals(Common().parseGoalMap(hash))
                            } catch (de: Exception) {
                                listener.onError(de)
                            }

                        }
                        .addOnFailureListener { e ->
                            listener.onError(e)
                        }
                } catch (e: Exception) {
                    listener.onError(e)
                }
            }
            "Goals Pause" -> {
                try {
                    db.getReference("goals")
                        .child(accountID)
                        .get()
                        .addOnSuccessListener { dataSnapShot ->
                            try {
                                var hash = dataSnapShot.getValue<Map<String, Object>>()!!
                                listener.onSuccessListOfGoals(Common().parseGoalMap(hash))
                            } catch (de: Exception) {
                                listener.onError(de)
                            }

                        }
                        .addOnFailureListener { e ->
                            listener.onError(e)
                        }
                } catch (e: Exception) {
                    listener.onError(e)
                }

            }
            "Goal History" -> {
                try {
                    db.getReference("goalHistory")
                        .child(accountID)
                        .get()
                        .addOnSuccessListener { dataSnapShot ->
                            try {
                                var hash = dataSnapShot.getValue<Map<String, Object>>()!!
                                Log.e("HASH", hash.toString())
                                listener.onSuccessListOfGoalHistory(Common().parseGoalsHistory(hash))
                            } catch (de: Exception) {
                                listener.onError(de)
                            }

                        }
                        .addOnFailureListener { e ->
                            listener.onError(e)
                        }
                } catch (e: Exception) {
                    listener.onError(e)
                }
            }
        }
    }

    fun deleteDataFromDB(
        entityName: String,
        accountID: String,
        key: String,
        listener: FirebaseDatabaseCallback
    ) {
        when (entityName) {
            "Goals" -> {
                try {
                    db.getReference("goals")
                        .child(accountID)
                        .child(key)
                        .removeValue()
                    listener.onSuccessInsert("")
                } catch (e: Exception) {
                    listener.onError(e)
                }
            }
            "Goal History" -> {
                try {
                    db.getReference("goalHistory")
                        .child(accountID)
                        .child(key)
                        .removeValue()
                    listener.onSuccessInsert(key)
                } catch (e: Exception) {
                    listener.onError(e)
                }
            }
            else -> {
                listener.onError(Exception("Not valid entity name"))
            }
        }
    }

}