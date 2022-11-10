package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.data.Loan
import java.text.DateFormat
import java.util.*

class LoanAdapter(
    private val loans: MutableList<Loan>,
    private val listener: LoanInterface

) : RecyclerView.Adapter<LoanAdapter.LoanViewHolder>() {

    class LoanViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        return LoanViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_loan,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentLoan = loans[position]

        holder.itemView.apply {
            findViewById<TextView>(R.id.tvItemLoanIcon).text = currentLoan.name?.get(0)?.uppercase()
            findViewById<TextView>(R.id.tvItemLoanName).text = currentLoan.name
            findViewById<TextView>(R.id.tvItemLoanNotes).text = currentLoan.notes

            // expense or income
            val loanState = determineLoanType(currentLoan.type)
            val amountColorRes = resources.getIdentifier(
                loanState["color"],
                "color",
                activity.packageName
            )

            val amountText = loanState["currency"] + String.format("%,.2f", currentLoan.amount)
            findViewById<TextView>(R.id.tvItemLoanAmount).text = amountText
            findViewById<TextView>(R.id.tvItemLoanAmount).setTextColor(ContextCompat.getColor(activity, amountColorRes))

            val formattedDate =
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
                    .format(Date(currentLoan.date!!))

            val dateText = "Due on $formattedDate"
            findViewById<TextView>(R.id.tvItemLoanDate).text = dateText
        }
    }

    override fun getItemCount(): Int {
        return loans.size
    }

    fun addLoan(loan: Loan) {
        loans.add(loan)
        notifyItemInserted(loans.size - 1)
    }

    private fun determineLoanType(type: Int): Map<String, String> {
        val currency: String
        val color: String
        when (type) {
            0 -> {
                currency = "-₱"
                color = "bright_red"
            }
            else -> {
                currency = "₱"
                color = "green_secondary"
            }
        }

        return mapOf("currency" to currency, "color" to color)
    }
}