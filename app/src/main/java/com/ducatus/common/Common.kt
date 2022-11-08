package com.ducatus.common

import android.util.Log
import com.ducatus.data.GoalHistory
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
        goalMap["colorName"] = goals.colorName as Object
        goalMap["icon"] = goals.icon as Object
        goalMap["status"] = goals.status as Object
        goalMap["dateGoalPaused"] = goals.dateGoalPaused as Object

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
            goal.colorName = data["colorName"].toString()
            goal.color = data["color"].toString().toInt()
            goal.icon = data["icon"].toString().toInt()
            goal.status = data["status"].toString().toInt()
            goal.dateGoalPaused = data["dateGoalPaused"].toString()
            list.add(goal)
        }
        return list
    }

    fun toGoalHistory(goalHistory: GoalHistory): Map<String, Object> {
        var goalMap = HashMap<String, Object>()
        goalMap["accountID"] = goalHistory.accountID as Object
        goalMap["goalkey"] = goalHistory.goalkey as Object
        goalMap["goalHistoryKey"] = goalHistory.goalHistoryKey as Object
        goalMap["datePaid"] = goalHistory.datePaid as Object
        goalMap["timePaid"] = goalHistory.timePaid as Object
        goalMap["amountPaid"] = goalHistory.amountPaid as Object

        return goalMap
    }

    fun parseGoalsHistory(hash: Map<String, Object>): List<GoalHistory> {
        var list = mutableListOf<GoalHistory>()
        var hashData = hash.values
        for (goalHistoryData in hashData) {
            var data = goalHistoryData as Map<String, Object>
            var goalHistory = GoalHistory()
            goalHistory.accountID = data["accountID"].toString()
            goalHistory.goalkey = data["goalkey"].toString()
            goalHistory.goalHistoryKey = data["goalHistoryKey"].toString()
            goalHistory.datePaid = data["datePaid"].toString()
            goalHistory.timePaid = data["timePaid"].toString()
            goalHistory.amountPaid = data["amountPaid"].toString().toDouble()
            list.add(goalHistory)

        }
        return list
    }
}

