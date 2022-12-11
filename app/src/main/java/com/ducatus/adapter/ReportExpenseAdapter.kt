package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.HomeOverviewInterface
import com.ducatus.R
import com.ducatus.data.Expense
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ReportExpenseAdapter(
    private val expenses: MutableList<Expense>,
    private val listener: HomeOverviewInterface

) : RecyclerView.Adapter<ReportExpenseAdapter.ReportExpenseViewHolder>() {

    class ReportExpenseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportExpenseViewHolder {
        return ReportExpenseViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_report_expense,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ReportExpenseViewHolder, position: Int) {
        val currentExpense = expenses[position]

        holder.itemView.apply {
            findViewById<LinearLayout>(R.id.llItemReportExpense).setOnClickListener {
                when (currentExpense.type) {
                    'S' -> listener.viewItem('S', currentExpense.id!!)
                    'T' -> listener.viewItem('T', currentExpense.gsonObject!!)
                }
            }

            var itemColor = currentExpense.categoryColor!!
            var itemIcon = currentExpense.categoryIcon!!
            var itemName = currentExpense.categoryName!!

            // check if item has subcategory data
            if (currentExpense.subcategoryName != null) {
                itemColor = currentExpense.subcategoryColor!!
                itemIcon = currentExpense.subcategoryIcon!!
                itemName = currentExpense.subcategoryName
            }

            val iconColor = resources.getIdentifier(
                itemColor,
                "color",
                context.packageName
            )

            val icon = resources.getIdentifier(
                itemIcon,
                "drawable",
                context.packageName
            )

            findViewById<FrameLayout>(R.id.flItemReportExpenseIcon).backgroundTintList =
                ContextCompat.getColorStateList(context, iconColor)

            findViewById<ImageView>(R.id.ivItemReportExpenseIcon).setImageResource(icon)
            findViewById<ImageView>(R.id.ivItemReportExpenseIcon).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemReportExpenseName).text = currentExpense.name
            findViewById<TextView>(R.id.tvItemReportExpenseCategory).text = itemName
            findViewById<TextView>(R.id.tvItemReportExpenseType).text = currentExpense.paymentType

            val amountText = "-₱" + String.format("%,.2f", currentExpense.amount)
            findViewById<TextView>(R.id.tvItemReportExpenseAmount).text = amountText
            findViewById<TextView>(R.id.tvItemReportExpenseAmount).setTextColor(
                ContextCompat.getColor(context, R.color.bright_red)
            )

            val dateText = determineDateText(currentExpense.date!!)
            findViewById<TextView>(R.id.tvItemReportExpenseDate).text = dateText["date"]
            findViewById<TextView>(R.id.tvItemReportExpenseTime).text = dateText["time"]
        }
    }

    override fun getItemCount(): Int {
        return expenses.size
    }

    fun addExpense(expense: Expense) {
        expenses.add(expense)
        notifyItemInserted(expenses.size - 1)
    }

    private fun determineDateText(date: Long): Map<String, String> {
        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneId.systemDefault()
        )
        val transactionDate = zdt.dayOfYear

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val today = zdtToday.dayOfYear

        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        val formattedDate = dtf.format(zdt)

        val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
        val formattedTime = dtf2.format(zdt)

        var dateText = formattedDate
        if (transactionDate == today) {
            dateText = "Today"
        }

        return mapOf("date" to dateText, "time" to formattedTime)
    }
}