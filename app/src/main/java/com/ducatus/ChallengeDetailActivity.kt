package com.ducatus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.ducatus.adapter.ChallengeHistoryAdapter
import com.ducatus.common.AppResources
import com.ducatus.data.Challenge
import com.ducatus.data.ChallengeHistory
import com.ducatus.databinding.ActivityChallengeDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ChallengeDetailActivity : AppCompatActivity() {
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityChallengeDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var challengesHistory: MutableList<ChallengeHistory>
    private lateinit var selectedChallenge: Challenge
    private var firebaseUser: FirebaseUser? = null
    private var currentChallengePosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChallengeDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadData()

        binding.tbChallengeDetail.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        firebaseUser?.run {
            sharedPreferences = SharedPreferences(applicationContext)

            val strChallenge = intent.getStringExtra("challenge")
            selectedChallenge = Gson().fromJson(strChallenge, Challenge::class.java)

            database = Firebase.database
            databaseReference =
                database.getReference("challengeHistory")
                    .child(uid)
                    .child(sharedPreferences.accountId!!)
                    .child(selectedChallenge.id.toString())

            binding.tvChallengeDetailTitle.text = selectedChallenge.title
            loadChallengeHistoryInitialState(selectedChallenge.id)

        } ?: sessionExpired()
    }

    private fun loadChallengeInitialState(challengeId: Int) {
        val challenges = AppResources().getChallenges()
        val challenge = challenges[challengeId]
        if (challenge != null) {
            selectedChallenge = challenge
            loadChallengeHistoryInitialState(challengeId)
        }
    }

    private fun loadChallengeHistoryInitialState(challengeId: Int) {
        showProgressDialog()
        challengesHistory = mutableListOf()
        val challengeAmountList = AppResources().getChallengesAmounts()
        val challengeAmount = challengeAmountList[challengeId]

        if (challengeAmount != null) {
            for (i in challengeAmount.indices) {
                challengesHistory.add(
                    ChallengeHistory(
                        "",
                        challengeAmount[i],
                        null,
                        i,
                        challengeId,
                    )
                )
            }

            loadChallengeHistory(challengesHistory)
        }
        else {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.challenge_empty_title))
                .setPositiveButton(resources.getString(R.string.go_back)) { _, _ -> onBackPressed() }
                .setOnDismissListener { onBackPressed() }
                .show()
        }
    }

    private fun loadChallengeHistory(challengesHistory: MutableList<ChallengeHistory>) {
        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                var daysSaved = 0
                for (child in snapshot.children) {
                    val challengeHistory = child.getValue<ChallengeHistory>()
                    challengeHistory?.let {
                        challengesHistory[it.position].datePaid = it.datePaid
                        daysSaved++
                    }
                }

                challengesHistory.sortBy { it.position }

                val spanCount = when (selectedChallenge.duration) {
                    7 -> 4
                    14 -> 4
                    else -> 6
                }

                currentChallengePosition = 0
                selectedChallenge.dateStarted?.let {
                    currentChallengePosition = determineCurrentPosition(it)
                }

                // check if current position is same as challenge duration
                // true -> challenge finished; disable text field and button
                // false -> challenge ongoing; set amount
                if (currentChallengePosition >= selectedChallenge.duration - 1) {
                    val challengeAmount = "₱${selectedChallenge.challengeAmount}"
                    val savedAmount = "₱${selectedChallenge.savedAmount}"
                    val missedAmount = "₱${selectedChallenge.challengeAmount - selectedChallenge.savedAmount}"
                    val amountText = "You have saved $savedAmount out of $challengeAmount"

                    binding.tvChallengeDetailTotalEarned.text = amountText

                    val savedText =
                        if (daysSaved == 1) "$daysSaved day saved"
                        else "$daysSaved days saved"

                    binding.tvChallengeDetailDaysSaved.text = savedText
                    binding.tvChallengeDetailSavedAmount.text = savedAmount

                    val daysMissed = selectedChallenge.duration - daysSaved
                    val missedText =
                        if (daysMissed == 1) "$daysMissed day missed"
                        else "$daysMissed days missed"

                    binding.tvChallengeDetailDaysMissed.text = missedText
                    binding.tvChallengeDetailMissedAmount.text = missedAmount

                    hideSaveAmount()
                    showSummary()

                    binding.btnChallengeDetailRestart.setOnClickListener {
                        confirmRestart(selectedChallenge.id)
                    }
                }
                else {
                    hideSummary()

                    // set amount in text field
                    val amountText = challengesHistory[currentChallengePosition].amount.toString()
                    binding.tfChallengeDetailSavedAmount.editText?.setText(amountText)

                    if (challengesHistory[currentChallengePosition].datePaid == null) {
                        enableSaveAmount()
                        showSaveAmount()

                        binding.btnChallengeDetailSave.setOnClickListener {
                            saveAmount()
                        }
                    }
                    else {
                        showSaveAmount()
                        disableSaveAmount()
                    }
                }

                val challengeHistoryAdapter = ChallengeHistoryAdapter(challengesHistory, currentChallengePosition)
                binding.rvChallengeHistory.adapter = challengeHistoryAdapter
                binding.rvChallengeHistory.layoutManager =
                    GridLayoutManager(this, spanCount, GridLayoutManager.VERTICAL, false)

                hideProgressDialog()
            }
            .addOnFailureListener {
                hideProgressDialog()
                Snackbar
                    .make(binding.clChallengeDetail, getString(R.string.load_challenge_history_error),5000)
                    .show()
            }
    }

    private fun determineCurrentPosition(startDate: Long): Int {
        val zdtStartDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(startDate),
            ZoneId.systemDefault()
        ).dayOfYear

        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        ).dayOfYear

        return zdtToday - zdtStartDate
    }

    private fun saveAmount() {
        showProgressDialogAction(getString(R.string.saving))
        val amount = binding.tfChallengeDetailSavedAmount.editText?.text.toString().trim { it <= ' ' }
        val zdtToday = ZonedDateTime.ofInstant(
            Instant.now(),
            ZoneId.systemDefault()
        ).toInstant().toEpochMilli()

        val key = databaseReference.push().key!!
        val challengeHistory = ChallengeHistory(
            key,
            amount.toInt(),
            zdtToday,
            currentChallengePosition,
            selectedChallenge.id
        )

        databaseReference.child(key).setValue(challengeHistory)
            .addOnSuccessListener {
                cancelNotification(this, sharedPreferences.accountId!!)
                hideProgressDialogAction()

                selectedChallenge.savedAmount += amount.toInt()
                loadChallengeHistory(challengesHistory)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clChallengeDetail, getString(R.string.save_amount_error),5000)
                    .show()
            }
    }

    private fun cancelNotification(context: Context, accountId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.challengesChannelId)
        if (notificationChannel != null) {
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            notificationIntent.action = "com.ducatus.CHALLENGE"
            val notificationId = selectedChallenge.id

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            // schedule future notifications
            if (notificationChannel.importance != NotificationManager.IMPORTANCE_NONE) {
                scheduleNotification(context, accountId)
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Challenges"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(sharedPreferences.challengesChannelId, name, importance)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun enableReceiver(context: Context) {
        val receiver = ComponentName(context, NotificationReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun scheduleNotification(context: Context, accountId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.challengesChannelId)
        if (notificationChannel == null) {
            createNotificationChannel()
            notificationChannel = notificationManager.getNotificationChannel(sharedPreferences.challengesChannelId)
        }

        // create notification if channel is enabled
        // else do not create
        if (notificationChannel.importance != NotificationManager.IMPORTANCE_NONE) {
            enableReceiver(context)

            // pass to broadcast receiver
            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val title = "Complete your challenge for today"
            val message = "Tap here to open Ducatus."

            val notificationId = selectedChallenge.id
            notificationIntent.action = "com.ducatus.CHALLENGE"
            notificationIntent.putExtra(titleExtra, title)
            notificationIntent.putExtra(messageExtra, message)
            notificationIntent.putExtra(notificationIdExtra, notificationId)
            notificationIntent.putExtra(accountIdExtra, accountId)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // six pm every day
            val zdtNow = ZonedDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            ).with(LocalTime.MIN).plusHours(18).plusDays(1).toInstant().toEpochMilli()

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                zdtNow,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    private fun confirmRestart(challengeId: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.restart_challenge_title))
            .setPositiveButton(resources.getString(R.string.restart)) { _, _ -> restartChallenge(challengeId) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun restartChallenge(challengeId: Int) {
        showProgressDialogAction(getString(R.string.restarting_challenge))
        databaseReference.removeValue()
            .addOnSuccessListener {
                hideProgressDialogAction()
                loadChallengeInitialState(challengeId)
            }
            .addOnFailureListener {
                hideProgressDialogAction()
                Snackbar
                    .make(binding.clChallengeDetail, getString(R.string.restart_challenge_error), 5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun enableSaveAmount() {
        binding.tfChallengeDetailSavedAmount.editText?.setTextColor(
            ContextCompat.getColor(this, R.color.off_black)
        )
        binding.btnChallengeDetailSave.isClickable = true
        binding.btnChallengeDetailSave.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.bright_green)
    }

    private fun disableSaveAmount() {
        binding.tvChallengeDetailCompletedToday.visibility = View.VISIBLE
        binding.tfChallengeDetailSavedAmount.editText?.setTextColor(
            ContextCompat.getColor(this, R.color.lighter_gray)
        )
        binding.btnChallengeDetailSave.isClickable = false
        binding.btnChallengeDetailSave.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.gray)
    }

    private fun showSaveAmount() {
        binding.tfChallengeDetailSavedAmount.visibility = View.VISIBLE
        binding.btnChallengeDetailSave.visibility = View.VISIBLE
    }

    private fun hideSaveAmount() {
        binding.tvChallengeDetailCompletedToday.visibility = View.GONE
        binding.tfChallengeDetailSavedAmount.visibility = View.GONE
        binding.btnChallengeDetailSave.visibility = View.GONE
    }

    private fun showSummary() {
        binding.tvChallengeDetailTotalEarned.visibility = View.VISIBLE
        binding.llChallengeDetailSaved.visibility = View.VISIBLE
        binding.llChallengeDetailMissed.visibility = View.VISIBLE
        binding.btnChallengeDetailRestart.visibility = View.VISIBLE
    }

    private fun hideSummary() {
        binding.tvChallengeDetailTotalEarned.visibility = View.GONE
        binding.llChallengeDetailSaved.visibility = View.GONE
        binding.llChallengeDetailMissed.visibility = View.GONE
        binding.btnChallengeDetailRestart.visibility = View.GONE
    }

    private fun showProgressDialog() {
        binding.pbChallengeDetail.visibility = View.VISIBLE
        binding.llChallengeDetail.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbChallengeDetail.visibility = View.GONE
        binding.llChallengeDetail.visibility = View.VISIBLE
    }

    private fun showProgressDialogAction(title: String) {
        val bundle = Bundle()
        bundle.putString("title", title)

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(supportFragmentManager, "dialog")
    }

    private fun hideProgressDialogAction() {
        actionDialog.dismiss()
    }
}