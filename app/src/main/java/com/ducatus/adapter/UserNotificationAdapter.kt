package com.ducatus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.R
import com.ducatus.data.UserNotification
import com.ducatus.interfaces.UserNotificationInterface
import java.text.SimpleDateFormat
import java.util.*

class UserNotificationAdapter(
    private val userNotifications: MutableList<UserNotification>,
    private val listener: UserNotificationInterface

) : RecyclerView.Adapter<UserNotificationAdapter.UserNotificationViewHolder>() {

    class UserNotificationViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserNotificationViewHolder {
        return UserNotificationViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_notification,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UserNotificationViewHolder, position: Int) {
        val currentUserNotification = userNotifications[position]

        holder.itemView.apply {
            findViewById<LinearLayout>(R.id.llItemNotificationInfo).setOnClickListener {
                val type = currentUserNotification.type?.let {
                    if (it != "loan" && it != "subscription") it
                    else it + "Id"
                }

                listener.viewItem(type!!, currentUserNotification.itemId)
            }

            when (currentUserNotification.type) {
                "challenge" -> {
                    val icon = R.drawable.ic_challenges
                    findViewById<ImageView>(R.id.ivItemNotificationIcon).setImageResource(icon)
                }
                "expense" -> {
                    val icon = R.drawable.ic_local_transactions_24
                    findViewById<ImageView>(R.id.ivItemNotificationIcon).setImageResource(icon)
                }
                "loan" -> {
                    val icon = R.drawable.ic_baseline_handshake_24
                    findViewById<ImageView>(R.id.ivItemNotificationIcon).setImageResource(icon)
                }
                "subscription" -> {
                    val icon = R.drawable.ic_baseline_calendar_today_24
                    findViewById<ImageView>(R.id.ivItemNotificationIcon).setImageResource(icon)
                }
            }

            findViewById<TextView>(R.id.tvItemNotificationTitle).text = currentUserNotification.title
            findViewById<TextView>(R.id.tvItemNotificationMessage).text = currentUserNotification.message

            val dateFormat = SimpleDateFormat("MMM d", Locale.US)
            val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

            val formattedDate = dateFormat.format(Date(currentUserNotification.notifiedAt!!))
            val formattedTime = timeFormat.format(Date(currentUserNotification.notifiedAt!!))

            val dateText = "$formattedDate at $formattedTime"
            findViewById<TextView>(R.id.tvItemNotificationDate).text = dateText
        }
    }

    override fun getItemCount(): Int {
        return userNotifications.size
    }

    fun addUserNotification(userNotification: UserNotification) {
        userNotifications.add(userNotification)
        notifyItemInserted(userNotifications.size - 1)
    }
}