package com.ducatus.services

import android.util.Log
import com.ducatus.common.Common
import com.ducatus.data.Account
import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import okhttp3.internal.Util
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LocalFirebaseDatabase {

    private val db: FirebaseDatabase = FirebaseDatabase.getInstance();

    constructor()

    fun writeToDb(entities: LocalEntities, entityName: String, listener: FirebaseDatabaseCallback) {
        when (entityName) {
            "Goals" -> {
                val goals: Goals = entities.goals

                val key = db.getReference("goals")
                    .child(goals.accountID).push().key!!
                goals.key = key
                val finalMap = Common().toGoalsMap(goals)
                db.getReference("goals")
                    .child(goals.accountID)
                    .child(key)
                    .setValue(finalMap)
                    .addOnSuccessListener {
                        listener.onSuccessInsert()
                    }
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
        }
    }

}