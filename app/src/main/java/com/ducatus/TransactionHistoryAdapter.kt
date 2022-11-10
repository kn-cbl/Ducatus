package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.data.Transaction
import java.text.DateFormat
import java.util.*

class TransactionHistoryAdapter(
    private val transactions: MutableList<Transaction>,
    private val listener: TransactionHistoryInterface

) : RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        return TransactionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_transaction_history,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentTransaction = transactions[position]

        holder.itemView.apply {
            val formattedDate =
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                    .format(Date(currentTransaction.date!!))

            val meridian: String
            val hour = (currentTransaction.hour!! / 1000 / 60).toString().toInt()
            val minute = (currentTransaction.minute!! / 1000 / 60).toString().toInt()

            val formattedHour: Int
            if (hour > 12) {
                formattedHour = hour - 12
                meridian = "PM"
            }
            else if (hour == 12) {
                formattedHour = hour
                meridian = "PM"
            }
            else if (hour == 0) {
                formattedHour = hour + 12
                meridian = "AM"
            }
            else { // < 12
                formattedHour = hour
                meridian = "AM"
            }

            val formattedMinute =
                if (minute > 9) minute
                else "0${minute}"

            val dateTime = "$formattedDate at $formattedHour:$formattedMinute $meridian"
            findViewById<TextView>(R.id.tvTransactionHistoryDate).text = dateTime

            when (currentTransaction.receipt) {
                null -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryReceipt).text = context.getString(R.string.no_receipt)
                }
                else -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryReceipt).text = context.getString(R.string.receipt_attached)
                    findViewById<TextView>(R.id.ivTransactionHistoryReceipt).visibility = View.VISIBLE
                }
            }

            val amount = "₱" + String.format("%,.2f", currentTransaction.amount)
            findViewById<TextView>(R.id.tvTransactionHistoryAmount).text = amount

            when (currentTransaction.type) {
                0 -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryAmount).setTextColor(
                        ContextCompat.getColor(activity, R.color.bright_red)
                    )
                }
                else -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryAmount).setTextColor(
                        ContextCompat.getColor(activity, R.color.green_secondary)
                    )
                }
            }

            findViewById<TextView>(R.id.tvTransactionHistoryPaymentType).text = currentTransaction.paymentType
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        notifyItemInserted(transactions.size - 1)
    }
}