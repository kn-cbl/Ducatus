package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.GoalHistory
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GoalHistoryAdapter(
    private val goalsHistory: MutableList<GoalHistory>,

    ) : RecyclerView.Adapter<GoalHistoryAdapter.GoalHistoryViewHolder>() {

    class GoalHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalHistoryViewHolder {
        return GoalHistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_goal_history,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: GoalHistoryViewHolder, position: Int) {
        val currentGoalHistory = goalsHistory[position]

        holder.itemView.apply {
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(currentGoalHistory.date),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)

            val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = dtf2.format(zdt)

            findViewById<TextView>(R.id.tvItemGoalHistoryDate).text = formattedDate
            findViewById<TextView>(R.id.tvItemGoalHistoryTime).text = formattedTime

            val amountText = "₱" + String.format("%,.2f", currentGoalHistory.amount)
            findViewById<TextView>(R.id.tvItemGoalHistoryAmount).text = amountText
        }
    }

    override fun getItemCount(): Int {
        return goalsHistory.size
    }

    fun addGoalHistory(goalHistory: GoalHistory) {
        goalsHistory.add(goalHistory)
        notifyItemInserted(goalsHistory.size - 1)
    }
}