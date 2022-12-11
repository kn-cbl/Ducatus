package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.LoanInterface
import com.ducatus.R
import com.ducatus.data.Loan
import com.google.android.material.divider.MaterialDivider
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LoanFullyPaidAdapter(
    private val loans: MutableList<Loan>,
    private val listener: LoanInterface

) : RecyclerView.Adapter<LoanFullyPaidAdapter.LoanFullyPaidViewHolder>() {

    class LoanFullyPaidViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanFullyPaidViewHolder {
        return LoanFullyPaidViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_loan,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LoanFullyPaidViewHolder, position: Int) {
        val currentLoanFullyPaid = loans[position]

        holder.itemView.apply {
            findViewById<TextView>(R.id.tvItemLoanIcon).text = currentLoanFullyPaid.name?.get(0)?.uppercase()
            findViewById<TextView>(R.id.tvItemLoanName).text = currentLoanFullyPaid.name

            val notesText = currentLoanFullyPaid.notes ?: resources.getString(R.string.notes_empty)
            findViewById<TextView>(R.id.tvItemLoanNotes).text = notesText

            findViewById<TextView>(R.id.tvItemLoanAmount).visibility = View.GONE

            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(currentLoanFullyPaid.paidAt!!),
                ZoneId.systemDefault()
            )

            val dtf = DateTimeFormatter.ofPattern("MM/dd/yy")
            val formattedDate = dtf.format(zdt)

            val dtf2 = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = dtf2.format(zdt)

            val dateText = "Paid at $formattedDate"
            findViewById<TextView>(R.id.tvItemLoanDate).text = dateText
            findViewById<TextView>(R.id.tvItemLoanTime).text = formattedTime

            findViewById<LinearLayout>(R.id.llItemLoan).setOnClickListener {
                listener.viewItem(currentLoanFullyPaid.id!!)
            }

            if (position == loans.size - 1) {
                findViewById<MaterialDivider>(R.id.mdItemLoan).visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return loans.size
    }

    fun addLoan(loan: Loan) {
        loans.add(loan)
        notifyItemInserted(loans.size - 1)
    }
}