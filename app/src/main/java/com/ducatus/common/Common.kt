package com.ducatus.common

import android.util.Log
import com.ducatus.data.Goals
import kotlinx.serialization.json.JsonArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class Common {
    fun toGoalsMap(goals: Goals): Map<String, Object> {
        var goalMap = hashMapOf<String, Object>()
        goalMap["key"] = goals.key as Object
        goalMap["accountID"] = goals.accountID as Object
        goalMap["goalDescription"] = goals.goalDescription as Object
        goalMap["targetDate"] = goals.targetDate as Object
        goalMap["percentage"] = goals.percentage as Object
        goalMap["earned"] = goals.earned as Object
        goalMap["remaining"] = goals.remaining as Object
        goalMap["goalAmount"] = goals.goalAmount as Object
        goalMap["notes"] = goals.notes as Object
        goalMap["color"] = goals.color as Object
        goalMap["icon"] = goals.icon as Object
        return goalMap
    }

    fun parseGoalMap(hash: Map<String, Object>): List<Goals> {
        var list = mutableListOf<Goals>()
        var hashData = hash.values
        for (goalData in hashData) {
            var data = goalData as Map<String, Object>
            var goal: Goals = Goals()
            goal.key = data["key"].toString()
            goal.accountID = data["accountID"].toString()
            goal.goalAmount = data["goalAmount"].toString().toDouble()
            goal.goalDescription = data["goalDescription"].toString()
            goal.targetDate = data["targetDate"].toString()
            goal.percentage = data["percentage"].toString().toDouble()
            goal.earned = data["earned"].toString().toDouble()
            goal.remaining = data["remaining"].toString().toDouble()
            goal.notes = data["notes"].toString()
            goal.color = data["color"].toString().toInt()
            goal.icon = data["icon"].toString().toInt()
            list.add(goal)
        }
        return list
    }
}

