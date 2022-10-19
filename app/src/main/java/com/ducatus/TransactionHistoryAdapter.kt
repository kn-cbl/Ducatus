package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

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
            val date = currentTransaction.transaction_date
            val time = currentTransaction.transaction_time
            val dateTime = "$date at $time"
            findViewById<TextView>(R.id.tvTransactionHistoryDate).text = dateTime

            when (currentTransaction.transaction_receipt) {
                null -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryReceipt).text = context.getString(R.string.no_receipt)
                }
                else -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryReceipt).text = context.getString(R.string.receipt_attached)
                    findViewById<TextView>(R.id.ivTransactionHistoryReceipt).visibility = View.VISIBLE
                }
            }

            val amount = "₱" + String.format("%,.2f", currentTransaction.transaction_amount)
            findViewById<TextView>(R.id.tvTransactionHistoryAmount).text = amount

            when (currentTransaction.transaction_type) {
                0 -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryAmount).setTextColor(
                        ContextCompat.getColor(activity, R.color.green_secondary)
                    )
                }
                else -> {
                    findViewById<TextView>(R.id.tvTransactionHistoryAmount).setTextColor(
                        ContextCompat.getColor(activity, R.color.dark_red)
                    )
                }
            }

            findViewById<TextView>(R.id.tvTransactionHistoryPaymentType).text = currentTransaction.transaction_payment_type
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