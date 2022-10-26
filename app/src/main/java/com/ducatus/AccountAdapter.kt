package com.ducatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.data.Account

class AccountAdapter(
    private val accounts: MutableList<Account>,
    private val listener: AccountInterface

) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    class AccountViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_account,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val activity = listener.getActivityInterface()
        val currentAccount = accounts[position]

        holder.itemView.apply {
            val imageColor = resources.getIdentifier(
                currentAccount.account_color.toString(),
                "color",
                activity.packageName
            )

            findViewById<ImageView>(R.id.ivItemAccountImage).setColorFilter(
                ResourcesCompat.getColor(
                    resources,
                    imageColor,
                    null
                )
            )

            findViewById<TextView>(R.id.tvItemAccountName).text = currentAccount.account_name

            val budget = "₱" + String.format("%,.2f", currentAccount.account_monthly_budget)
            findViewById<TextView>(R.id.tvItemAccountBudget).text = budget

            findViewById<ImageView>(R.id.ibItemAccountEdit).setOnClickListener {
                listener.showPopup(it, 2, currentAccount.account_id.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return accounts.size
    }

    fun addAccount(account: Account) {
        accounts.add(account)
        notifyItemInserted(accounts.size - 1)
    }
}