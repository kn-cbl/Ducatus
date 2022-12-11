package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.Goal
import com.ducatus.interfaces.GoalInterface
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GoalAdapter(
    private val goals: MutableList<Goal>,
    private val listener: GoalInterface

) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        return GoalViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_goal,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val currentGoal = goals[position]

        holder.itemView.apply {
            val activityDateText = when (currentGoal.updatedAt) {
                null -> resources.getString(R.string.no_activity)
                else -> {
                    val elapsedTime = getElapsedTime(currentGoal.updatedAt!!)
                    resources.getString(R.string.last_activity) + " $elapsedTime"
                }
            }
            findViewById<TextView>(R.id.tvItemGoalDate).text = activityDateText

            findViewById<MaterialCardView>(R.id.cvItemGoal).setOnClickListener {
                listener.viewItem(currentGoal)
            }

            val iconColor = resources.getIdentifier(
                currentGoal.color,
                "color",
                context.packageName
            )

            findViewById<FrameLayout>(R.id.flItemGoalIcon).backgroundTintList =
                ContextCompat.getColorStateList(context, iconColor)

            val icon = resources.getIdentifier(
                currentGoal.icon,
                "drawable",
                context.packageName
            )

            findViewById<ImageView>(R.id.ivItemGoalIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemGoalIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemGoalName).text = currentGoal.name

            val goalAmount = currentGoal.targetAmount
            val goalSaved = currentGoal.savedAmount
            val goalRemaining = goalAmount.minus(goalSaved)

            val goalSavedText = "₱" + String.format("%,.2f", goalSaved)
            findViewById<TextView>(R.id.tvItemGoalSaved).text = goalSavedText
            findViewById<TextView>(R.id.tvItemGoalSaved).setTextColor(
                ContextCompat.getColor(context, iconColor)
            )

            val goalRemainingText = "₱" + String.format("%,.2f", goalRemaining)
            findViewById<TextView>(R.id.tvItemGoalRemaining).text = goalRemainingText
            findViewById<TextView>(R.id.tvItemGoalRemaining).setTextColor(
                ContextCompat.getColor(context, iconColor)
            )

            val goalAmountText = "₱" + String.format("%,.2f", goalAmount)
            findViewById<TextView>(R.id.tvItemGoalAmount).text = goalAmountText
            findViewById<TextView>(R.id.tvItemGoalAmount).setTextColor(
                ContextCompat.getColor(context, iconColor)
            )

            val goalProgress = ((goalSaved / goalAmount) * 100).toInt()
            findViewById<LinearProgressIndicator>(R.id.pbItemGoal).progress = goalProgress
            findViewById<LinearProgressIndicator>(R.id.pbItemGoal).setIndicatorColor(
                ContextCompat.getColor(context, iconColor)
            )

            val progressText = "$goalProgress%"
            findViewById<TextView>(R.id.tvItemGoalProgress).text = progressText

            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            var zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(currentGoal.targetDate),
                ZoneId.systemDefault()
            )
            var formattedDate = dtf.format(zdt)
            var dateText = "Target date: $formattedDate"

            if (currentGoal.status != "R") {
                val overdueText = isOverdue(currentGoal.targetDate)
                if (overdueText != null) {
                    findViewById<TextView>(R.id.tvItemGoalOverdue).text = overdueText
                }
                else {
                    findViewById<TextView>(R.id.tvItemGoalOverdue).visibility = View.GONE
                }
            }
            else {
                zdt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(currentGoal.reachedDate!!),
                    ZoneId.systemDefault()
                )
                formattedDate = dtf.format(zdt)
                dateText = "Reached: $formattedDate"

                findViewById<TextView>(R.id.tvItemGoalOverdue).visibility = View.GONE
            }

            findViewById<TextView>(R.id.tvItemGoalTargetDate).text = dateText
        }
    }

    override fun getItemCount(): Int {
        return goals.size
    }

    fun addGoal(goal: Goal) {
        goals.add(goal)
        notifyItemInserted(goals.size - 1)
    }

    private fun getElapsedTime(date: Long): String {
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneId.systemDefault()
        )

        val startDate = zdt.toInstant()
        val endDate = zdtToday.toInstant()

        val elapsedDays = ChronoUnit.DAYS.between(startDate, endDate)
        val elapsedHours = ChronoUnit.HOURS.between(startDate, endDate)
        val elapsedMinutes = ChronoUnit.MINUTES.between(startDate, endDate)

        val dateText =
            if (elapsedDays > 0) {
                "${elapsedDays}d ago"
            }
            else {
                if (elapsedHours > 0) {
                    "${elapsedHours}h ago"
                }
                else {
                    "${elapsedMinutes}min. ago"
                }
            }

        return dateText
    }

    private fun isOverdue(targetDate: Long): String? {
        var dateText: String? = null

        val startDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(targetDate),
            ZoneId.systemDefault()
        )
        val endDate = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )

        val startEpoch = startDate.toInstant().toEpochMilli()
        val endEpoch = endDate.toInstant().toEpochMilli()

        if (startEpoch < endEpoch) {
            val elapsedYears = ChronoUnit.YEARS.between(startDate, endDate)
            val elapsedMonths = ChronoUnit.MONTHS.between(startDate, endDate)
            val elapsedDays = ChronoUnit.DAYS.between(startDate, endDate)
            val elapsedHours = ChronoUnit.HOURS.between(startDate, endDate)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startDate, endDate)
            val elapsedSeconds = ChronoUnit.SECONDS.between(startDate, endDate)

            dateText =
                if (elapsedYears > 0) {
                    if (elapsedYears.toInt() == 1) {
                        "$elapsedYears year overdue"
                    }
                    else {
                        "$elapsedYears years overdue"
                    }
                }
                else if (elapsedMonths > 0) {
                    if (elapsedMonths.toInt() == 1) {
                        "$elapsedMonths month overdue"
                    }
                    else {
                        "$elapsedMonths months overdue"
                    }
                }
                else if (elapsedDays > 0) {
                    if (elapsedDays.toInt() == 1) {
                        "$elapsedDays day overdue"
                    }
                    else {
                        "$elapsedDays days overdue"
                    }
                }
                else if (elapsedHours > 0) {
                    if (elapsedHours.toInt() == 1) {
                        "$elapsedHours hour overdue"
                    }
                    else {
                        "$elapsedHours hours overdue"
                    }
                }
                else if (elapsedMinutes > 0) {
                    if (elapsedMinutes.toInt() == 1) {
                        "$elapsedMinutes minutes overdue"
                    }
                    else {
                        "$elapsedMinutes minute overdue"
                    }
                }
                else {
                    if (elapsedSeconds.toInt() == 1) {
                        "$elapsedSeconds second overdue"
                    }
                    else {
                        "$elapsedSeconds seconds overdue"
                    }
                }
        }

        return dateText
    }
}