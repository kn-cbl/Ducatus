package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.GoalHistory

class GoalHistoryAdapter(private var context: Context, private var list: List<GoalHistory>) :
    RecyclerView.Adapter<GoalHistoryAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtSavedAmount: TextView = itemView.findViewById(R.id.goalAmount_goalHistory1)
        var txtDatePaid: TextView = itemView.findViewById(R.id.goalDate_goalHistory1)
        var txtTimePaid: TextView = itemView.findViewById(R.id.goalTime_goalHistory1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(context).inflate(R.layout.goal_detail_history, parent, false)

        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goalHistory = list.get(position)
        holder.txtDatePaid.text = goalHistory.datePaid
        holder.txtSavedAmount.text = "P" + goalHistory.amountPaid.toString()
        holder.txtTimePaid.text = goalHistory.timePaid
    }

    override fun getItemCount(): Int {
        return list.size
    }
}