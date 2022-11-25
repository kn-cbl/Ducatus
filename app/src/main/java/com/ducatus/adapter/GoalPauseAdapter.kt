package com.ducatus.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.adapter.GoalPauseAdapter.ViewHolder
import com.ducatus.data.Goals
import com.ducatus.interfaces.PauseGoalIntf
import java.time.LocalDate

class GoalPauseAdapter(
    private var context: Context,
    private var list: List<Goals>,
    private var listener: PauseGoalIntf
) :
    RecyclerView.Adapter<ViewHolder>() {


    class ViewHolder(itemView: View, mListener: PauseGoalIntf) : RecyclerView.ViewHolder(itemView) {
        var txtDescription: TextView = itemView.findViewById(R.id.textView_goalPaused1)
        var txtPercentage: TextView = itemView.findViewById(R.id.percentProgress_goalPaused1)
        var txtEarned: TextView = itemView.findViewById(R.id.earnedAmount_goalPaused1)
        var txtRemaining: TextView = itemView.findViewById(R.id.remainingAmount_goalPaused1)
        var txtGoalAmount: TextView = itemView.findViewById(R.id.goalAmount_goalPaused1)
        var txtTargetDate: TextView = itemView.findViewById(R.id.targetDate_goalPaused1)
        var imgCircle: ImageView = itemView.findViewById(R.id.sample_img_goalPaused1)
        var lowerPB: ProgressBar = itemView.findViewById(R.id.progressBar_goalPaused1)
        var txtDatePaused: TextView = itemView.findViewById(R.id.date_goalPaused1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView =
            LayoutInflater.from(context).inflate(R.layout.fragment_goal_paused, parent, false)

        return ViewHolder(mView, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goals = list.get(position)
        holder.txtDescription.setText(goals.goalDescription)
        holder.txtPercentage.setText(String.format("%.2f", goals.percentage) + "%")
        holder.lowerPB.progress = goals.percentage.toInt()
        holder.txtTargetDate.setText("Target Date on " + goals.targetDate)
        holder.txtGoalAmount.setText(String.format("P%.2f", goals.goalAmount))
        holder.txtEarned.setText(String.format("P%.2f", goals.earned))
        holder.txtRemaining.setText(String.format("P%.2f", goals.remaining))
        holder.imgCircle.setBackgroundColor(goals.color)
        holder.imgCircle.setImageResource(goals.icon)
        holder.txtDatePaused.setText(
            LocalDate.parse(goals.dateGoalPaused).month.toString()
                .substring(0, 3) + " " + LocalDate.parse(goals.dateGoalPaused).year.toString()
        )
        holder.itemView.setOnClickListener(View.OnClickListener { sView ->
            listener.OnClick(sView, position)
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }
}