package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.interfaces.AccountInterface
import com.ducatus.R
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
            val iconColor = resources.getIdentifier(
                currentAccount.color.toString(),
                "color",
                activity.packageName
            )

            findViewById<TextView>(R.id.tvItemAccountIcon).text = currentAccount.name?.get(0)?.uppercase()
            findViewById<FrameLayout>(R.id.flItemAccountIcon).backgroundTintList =
                ContextCompat.getColorStateList(activity, iconColor)

            findViewById<TextView>(R.id.tvItemAccountName).text = currentAccount.name

            val budget = "₱" + String.format("%,.2f", currentAccount.monthlyBudget)
            findViewById<TextView>(R.id.tvItemAccountBudget).text = budget

            findViewById<ImageView>(R.id.ibItemAccountEdit).setOnClickListener {
                listener.showPopup(it, 2, currentAccount.id.toString())
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