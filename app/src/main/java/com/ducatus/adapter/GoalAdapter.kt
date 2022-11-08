package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.adapter.GoalAdapter.ViewHolder
import com.ducatus.data.Goals
import com.ducatus.interfaces.ActiveGoalIntf

class GoalAdapter(
    private val context: Context,
    private val mList: List<Goals>,
    private val listener: ActiveGoalIntf
) :
    RecyclerView.Adapter<ViewHolder>() {
    class ViewHolder(itemView: View, listener: ActiveGoalIntf) : RecyclerView.ViewHolder(itemView) {
        var txtDescription: TextView = itemView.findViewById(R.id.textView_goalActive1)
        var txtPercentage: TextView = itemView.findViewById(R.id.percentProgress_goalActive1)
        var txtEarned: TextView = itemView.findViewById(R.id.earnedAmount_goalActive1)
        var txtRemaining: TextView = itemView.findViewById(R.id.remainingAmount_goalActive1)
        var txtGoalAmount: TextView = itemView.findViewById(R.id.goalAmount_goalActive1)
        var txtTargetDate: TextView = itemView.findViewById(R.id.targetDate_goalActive1)
        var imgCircle: ImageView = itemView.findViewById(R.id.sample_img_goalActive1)
        var lowerPB: ProgressBar = itemView.findViewById(R.id.progressBar_goalActive1)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var mView: View = LayoutInflater.from(context)
            .inflate(R.layout.fragment_goal_active, parent, false)

        return ViewHolder(mView, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var goal: Goals = mList.get(position)
        holder.txtDescription.text = goal.goalDescription
        holder.txtTargetDate.text = "Target date on " + goal.targetDate
        holder.txtGoalAmount.text = "P" + goal.goalAmount.toString()
        holder.txtEarned.text = "P" + goal.earned.toString()
        holder.txtRemaining.text = "P" + goal.remaining.toString()
        var percentage = goal.percentage.toInt()
        holder.txtPercentage.text = String.format("%.2f", goal.percentage) + "%"
        holder.lowerPB.progress = percentage
        holder.imgCircle.setBackgroundColor(goal.color)
        holder.imgCircle.setImageResource(goal.icon)
        holder.itemView.setOnClickListener { mView ->
            listener.OnClickListener(mView, position)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}