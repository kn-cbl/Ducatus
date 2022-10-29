package com.ducatus.services

import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import okhttp3.internal.Util
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
                db.getReference().child("goals").child(goals.accountID).setValue(goals)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            listener.onSuccessInsert()
                        }
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
        var goalsList: List<Goals> = ArrayList<Goals>()
        when (entityName) {
            "Goals" -> {
                db.getReference().child(entityName).child(accountID)
                    .get()
                    .addOnSuccessListener { dataSnapShot ->
                        goalsList = dataSnapShot.getValue<List<Goals>>()!!
                        listener.onSuccessListOfGoals(goalsList)
                    }
            }
        }
    }

}