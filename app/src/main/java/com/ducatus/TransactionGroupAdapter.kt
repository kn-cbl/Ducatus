package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.data.TransactionGroup
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.DateFormat
import java.util.*

class TransactionGroupAdapter(
    private val transactionsGroup: MutableList<TransactionGroup>,
    private val listener: TransactionInterface

) : RecyclerView.Adapter<TransactionGroupAdapter.TransactionGroupViewHolder>() {

    class TransactionGroupViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionGroupViewHolder {
        return TransactionGroupViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_transaction_group,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TransactionGroupViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentTransactionGroup = transactionsGroup[position]

        holder.itemView.apply {
            var currency = "₱"
            var amount = currentTransactionGroup.amountTotal
            var amountColor = "green_secondary"

            if (amount < 0) {
                currency = "-₱"
                amount *= -1
                amountColor = "bright_red"
            }

            val amountColorRes = resources.getIdentifier(
                amountColor,
                "color",
                activity.packageName
            )

            val dateText = determineDateText(currentTransactionGroup.date!!)
            findViewById<TextView>(R.id.tvItemTransactionDate).text = dateText

            val text = currency + String.format("%,.2f", amount)
            findViewById<TextView>(R.id.tvItemTransactionTotal).text = text
            findViewById<TextView>(R.id.tvItemTransactionTotal)
                .setTextColor(ContextCompat.getColor(activity, amountColorRes))

            val transactionAdapter = currentTransactionGroup.adapter
            val childView = findViewById<RecyclerView>(R.id.rvTransactionGroup)
            childView.adapter = transactionAdapter
            childView.layoutManager = LinearLayoutManager(activity)

            for (transaction in currentTransactionGroup.transactions!!) {
                transactionAdapter.addTransaction(transaction)
            }
        }
    }

    override fun getItemCount(): Int {
        return transactionsGroup.size
    }

    fun addTransactionGroup(transactionGroup: TransactionGroup) {
        transactionsGroup.add(transactionGroup)
        notifyItemInserted(transactionsGroup.size - 1)
    }

    private fun determineDateText(date: Long): String {
        val formattedDate =
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                .format(Date(date))

        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = today

        val dateToday = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
            .format(Date(calendar.timeInMillis))

        var dateText = formattedDate
        if (formattedDate == dateToday) {
            dateText = "Today"
        }

        return dateText
    }
}