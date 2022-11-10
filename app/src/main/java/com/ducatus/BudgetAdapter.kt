package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.data.Budget
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.DateFormat
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
            val currentBudgetDate =
                DateFormat
                    .getDateInstance(DateFormat.MEDIUM, Locale.US)
                    .format(Date(currentBudget.createdAt!!.toLong() * 1000))

            if (position > 0) {
                val previousBudgetDate =
                    DateFormat
                        .getDateInstance(DateFormat.MEDIUM, Locale.US)
                        .format(Date(budgets[position - 1].createdAt!!.toLong() * 1000))

                if (previousBudgetDate == currentBudgetDate) {
                    findViewById<TextView>(R.id.tvItemBudgetDate).visibility = View.GONE
                }
                else {
                    findViewById<TextView>(R.id.tvItemBudgetDate).text = currentBudgetDate
                }
            }
            else {
                findViewById<TextView>(R.id.tvItemBudgetDate).text = currentBudgetDate
            }

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

            findViewById<TextView>(R.id.tvItemBudgetName).text = currentBudget.name
            findViewById<TextView>(R.id.tvItemBudgetCategory).text = currentBudget.categoryName

            val budgetTotal = currentBudget.amountTotal.toString().toDouble()
            val budgetSpent = currentBudget.amountSpent.toString().toDouble()
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
            findViewById<ImageView>(R.id.ibViewItemBudget).tag = currentBudget.id
            findViewById<ImageView>(R.id.ibViewItemBudget).setOnClickListener {
                listener.viewItem(currentBudget.id.toString())
            }

            // determine icon and text to display
            var statusIcon = ""
            var statusText = ""

            when (budgetProgress) {
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

            val statusIconRes = resources.getIdentifier(
                statusIcon,
                "drawable",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivItemBudgetStatus).setImageResource(statusIconRes)
            findViewById<TextView>(R.id.tvItemBudgetStatus).text = statusText
        }
    }

    override fun getItemCount(): Int {
        return budgets.size
    }

    fun addBudget(budget: Budget) {
        budgets.add(budget)
        notifyItemInserted(budgets.size - 1)
    }
}