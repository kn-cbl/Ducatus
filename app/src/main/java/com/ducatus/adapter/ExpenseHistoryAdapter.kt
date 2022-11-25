package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.ExpenseHistory
import com.ducatus.interfaces.ExpenseHistoryInterface
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ExpenseHistoryAdapter(
    private val expensesHistory: MutableList<ExpenseHistory>,
    private val listener: ExpenseHistoryInterface

) : RecyclerView.Adapter<ExpenseHistoryAdapter.ExpenseHistoryViewHolder>() {

    class ExpenseHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseHistoryViewHolder {
        return ExpenseHistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_expense_history,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ExpenseHistoryViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentExpenseHistory = expensesHistory[position]

        holder.itemView.apply {
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(currentExpenseHistory.date!!),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)

            val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = dtf2.format(zdt)

            val dateTime = "$formattedDate at $formattedTime"
            findViewById<TextView>(R.id.tvExpenseHistoryDate).text = dateTime

            when (currentExpenseHistory.imagePath) {
                null -> {
                    findViewById<TextView>(R.id.tvExpenseHistoryReceipt).text = context.getString(
                        R.string.no_receipt
                    )
                }
                else -> {
                    findViewById<TextView>(R.id.tvExpenseHistoryReceipt).text = context.getString(
                        R.string.receipt_attached
                    )
                    findViewById<TextView>(R.id.tvExpenseHistoryReceipt).setOnClickListener {
                        listener.viewImage(currentExpenseHistory.imagePath)
                    }

                    findViewById<ImageView>(R.id.ivExpenseHistoryReceipt).visibility = View.VISIBLE
                }
            }

            val amount = "₱" + String.format("%,.2f", currentExpenseHistory.amount)
            findViewById<TextView>(R.id.tvExpenseHistoryAmount).text = amount

            when (currentExpenseHistory.isExpense) {
                 true -> {
                    findViewById<TextView>(R.id.tvExpenseHistoryAmount).setTextColor(
                        ContextCompat.getColor(activity, R.color.bright_red)
                    )
                }
                else -> {
                    findViewById<TextView>(R.id.tvExpenseHistoryAmount).setTextColor(
                        ContextCompat.getColor(activity, R.color.green_secondary)
                    )
                }
            }

            findViewById<TextView>(R.id.tvExpenseHistoryPaymentType).text = currentExpenseHistory.paymentType
        }
    }

    override fun getItemCount(): Int {
        return expensesHistory.size
    }

    fun addExpenseHistory(expenseHistory: ExpenseHistory) {
        expensesHistory.add(expenseHistory)
        notifyItemInserted(expensesHistory.size - 1)
    }
}