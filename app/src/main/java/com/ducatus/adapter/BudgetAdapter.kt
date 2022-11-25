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
import com.ducatus.interfaces.BudgetInterface
import com.ducatus.R
import com.ducatus.data.Budget
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class BudgetAdapter(
    private val budgets: MutableList<Budget>,
    private val listener: BudgetInterface

) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        return BudgetViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_budget,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentBudget = budgets[position]

        holder.itemView.apply {
            val dateText = when (currentBudget.updatedAt) {
                null -> activity.getString(R.string.budget_date_no_activity)
                else -> {
                    val elapsedTime = getElapsedTime(currentBudget.updatedAt!!)
                    activity.getString(R.string.budget_date_last_activity) + " $elapsedTime"
                }
            }

            findViewById<TextView>(R.id.tvItemBudgetDate).text = dateText

            val iconColor = resources.getIdentifier(
                currentBudget.categoryColor,
                "color",
                activity.packageName
            )

            findViewById<FrameLayout>(R.id.flItemBudgetCategoryIcon).backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

            val icon = resources.getIdentifier(
                currentBudget.categoryIcon,
                "drawable",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivItemBudgetCategoryIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemBudgetCategoryIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemBudgetCategory).text = currentBudget.categoryName

            val budgetTotal = currentBudget.amountTotal
            val budgetSpent = currentBudget.amountSpent
            val budgetLeft = budgetTotal.minus(budgetSpent)

            val spentText = "₱" + String.format("%,.2f", budgetSpent)
            findViewById<TextView>(R.id.tvItemBudgetSpent).text = spentText
            findViewById<TextView>(R.id.tvItemBudgetSpent).setTextColor(
                ContextCompat.getColor(activity, iconColor)
            )

            val budgetLeftText = "₱" + String.format("%,.2f", budgetLeft)
            findViewById<TextView>(R.id.tvItemBudgetLeft).text = budgetLeftText
            findViewById<TextView>(R.id.tvItemBudgetLeft).setTextColor(
                ContextCompat.getColor(activity, iconColor)
            )

            val budgetTotalText = "₱" + String.format("%,.2f", budgetTotal)
            findViewById<TextView>(R.id.tvItemBudgetLimit).text = budgetTotalText
            findViewById<TextView>(R.id.tvItemBudgetLimit).setTextColor(
                ContextCompat.getColor(activity, iconColor)
            )

            val budgetProgress = ((budgetSpent / budgetTotal) * 100).toInt()
            findViewById<LinearProgressIndicator>(R.id.pbItemBudget).progress = budgetProgress
            findViewById<LinearProgressIndicator>(R.id.pbItemBudget).setIndicatorColor(ContextCompat.getColor(activity, iconColor))

            findViewById<ImageView>(R.id.ibViewItemBudget).setOnClickListener {
                listener.viewItem(currentBudget)
            }

            // determine icon and text to display
            val status = getBudgetStatus(budgetProgress)
            val statusIconRes = resources.getIdentifier(
                status["icon"],
                "drawable",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivItemBudgetStatus).setImageResource(statusIconRes)
            findViewById<TextView>(R.id.tvItemBudgetStatus).text = status["text"]
        }
    }

    override fun getItemCount(): Int {
        return budgets.size
    }

    fun addBudget(budget: Budget) {
        budgets.add(budget)
        notifyItemInserted(budgets.size - 1)
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

    private fun getBudgetStatus(progress: Int): Map<String, String> {
        var statusIcon = ""
        var statusText = ""

        when (progress) {
            in 0..59 -> {
                statusIcon = "ic_budget_status_1"
                statusText = "Your budget is on track"
            }
            in 60..99 -> {
                statusIcon = "ic_budget_status_2"
                statusText = "You have almost reached your budget limit"
            }
            100 -> {
                statusIcon = "ic_budget_status_3"
                statusText = "You have reached your budget limit"
            }
        }

        return mapOf("icon" to statusIcon, "text" to statusText)
    }
}