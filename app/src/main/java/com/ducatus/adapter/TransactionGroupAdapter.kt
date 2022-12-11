package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.interfaces.TransactionInterface
import com.ducatus.data.TransactionGroup
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TransactionGroupAdapter(
    private val transactionsGroup: MutableList<TransactionGroup>,

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
                context.packageName
            )

            val dateText = determineDateText(currentTransactionGroup.date!!)
            findViewById<TextView>(R.id.tvItemTransactionDate).text = dateText

            val text = currency + String.format("%,.2f", amount)
            findViewById<TextView>(R.id.tvItemTransactionTotal).text = text
            findViewById<TextView>(R.id.tvItemTransactionTotal)
                .setTextColor(ContextCompat.getColor(context, amountColorRes))

            val transactionAdapter = currentTransactionGroup.adapter
            val childView = findViewById<RecyclerView>(R.id.rvTransactionGroup)
            childView.adapter = transactionAdapter
            childView.layoutManager = LinearLayoutManager(context)

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
        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneId.systemDefault()
        )
        val groupDate = zdt.dayOfYear

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        )
        val today = zdtToday.dayOfYear

        val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
        val formattedDate = dtf.format(zdt)

        var dateText = formattedDate
        if (groupDate == today) {
            dateText = "Today"
        }

        return dateText
    }
}