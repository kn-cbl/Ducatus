package com.ducatus

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ducatus.data.UserNotification
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

const val titleExtra = "Title"
const val messageExtra = "Message"
const val notificationIdExtra = "Notification Id"
const val itemIdExtra = "Item Id"
const val accountIdExtra = "Account Id"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = SharedPreferences(context)

        when (intent.action) {
            "com.ducatus.EXPENSE" -> {
                val smallIcon = R.drawable.ic_local_transactions_24
                showNotification(
                    context,
                    intent,
                    sharedPreferences.expensesChannelId!!,
                    smallIcon,
                    "expense",
                )
            }
            "com.ducatus.LOAN" -> {
                val smallIcon = R.drawable.ic_baseline_handshake_24
                showNotification(
                    context,
                    intent,
                    sharedPreferences.loansChannelId!!,
                    smallIcon,
                    "loan",
                )
            }
            "com.ducatus.SUBSCRIPTION" -> {
                val smallIcon = R.drawable.ic_baseline_calendar_today_24
                showNotification(
                    context,
                    intent,
                    sharedPreferences.subscriptionsChannelId!!,
                    smallIcon,
                    "subscription",
                )
            }
        }
    }

    private fun showNotification(
        context: Context,
        intent: Intent,
        channelId: String,
        smallIcon: Int,
        type: String?,
    ) {
        val accountId = intent.getStringExtra(accountIdExtra)
        val itemId = intent.getStringExtra(itemIdExtra)

        val activityIntent = Intent(context, HomeActivity::class.java)
        activityIntent.putExtra("accountId", accountId)
        activityIntent.putExtra("itemId", itemId)
        activityIntent.putExtra("notification", type)

        // activityIntent is the activity to start upon clicking notification
        val activityPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(activityIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val title = intent.getStringExtra(titleExtra)
        val message = intent.getStringExtra(messageExtra)

        // save to database
        val userNotification = UserNotification(
            null,
            type,
            title,
            message,
            itemId,
            null,
        )
        saveNotification(userNotification, accountId.toString())

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(activityPendingIntent)
            .setTicker(title + message)
            .setAutoCancel(true)
            .setChannelId(channelId)
            .build()

        val notificationId = intent.getIntExtra(notificationIdExtra, 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun saveNotification(userNotification: UserNotification, accountId: String) {
        val auth = Firebase.auth
        auth.currentUser?.let {
            val database = Firebase.database
            val databaseReference =
                database.getReference("notifications")
                    .child(it.uid)
                    .child(accountId)

            val key = databaseReference.push().key!!
            userNotification.id = key

            val zdt = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            )

            userNotification.notifiedAt = zdt.toInstant().toEpochMilli()
            databaseReference.child(key).setValue(userNotification)
        }
    }
}