package com.ducatus

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.CurrentChallengesAdapter
import com.ducatus.adapter.NewChallengesAdapter
import com.ducatus.common.Common
import com.ducatus.interfaces.ChallengeDetailListener
import com.ducatus.data.ChallengeHistory
import com.ducatus.data.Challenges
import com.ducatus.interfaces.ChallengesIntf
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.NewChallengeIntf
import com.ducatus.services.LocalFirebaseDatabase
import kotlinx.android.synthetic.main.challengesactivity_main_container.*
import java.lang.Exception
import kotlin.collections.HashMap

class ChallengesMainActivity : AppCompatActivity() {

    lateinit var recyclerCurrentChallenges: RecyclerView
    lateinit var recyclerNewChallenges: RecyclerView
    lateinit var newChallengeAdapter: NewChallengesAdapter
    lateinit var currentChallengesAdapter: CurrentChallengesAdapter
    lateinit var listener: ChallengesIntf
    lateinit var presentSet: HashMap<String, Object>
    lateinit var listOfChallenge: List<Challenges>
    lateinit var db: LocalFirebaseDatabase
    lateinit var listOfChallengeHistory: MutableList<ChallengeHistory>
    lateinit var pdLoading: ProgressDialog
    var restartChallengeName: String = ""
    var accountID = ""

    companion object {
        lateinit var cIntf: ChallengesIntf
        fun start(context: Context, c: ChallengesIntf) {
            cIntf = c
            val intent = Intent(context, ChallengesMainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_right_short, R.anim.slide_out_right_short);
        setContentView(R.layout.challengesactivity_main_container)
        initView()
        initListeners()
    }

    private fun initListeners() {
        challengesToolbar.setOnClickListener(View.OnClickListener {
            listener.OnToolBarListener()
            finish()
        })
    }

    private fun initView() {
        listener = ChallengesMainActivity.cIntf
        presentSet = HashMap<String, Object>()
        pdLoading = ProgressDialog(this@ChallengesMainActivity)
        pdLoading.setMessage("Sending Request...")
        pdLoading.setCancelable(false)
        listOfChallengeHistory = mutableListOf<ChallengeHistory>()
        recyclerCurrentChallenges = findViewById(R.id.recyclerCurrentChallenges)
        recyclerNewChallenges = findViewById(R.id.recyclerNewChallenges)
        accountID = SharedPreferences(this).accountId.toString()
        db = LocalFirebaseDatabase()
        loadChallenges()
    }

    private fun loadChallenges() {
        listOfChallenge = emptyList()
        listOfChallengeHistory.clear()
        presentSet.clear()
        db.getAllDataFromDB("Challenge History", accountID, object : FirebaseDatabaseCallback {
            override fun onSuccessListOfChallengeHistory(chList: List<ChallengeHistory>) {
                for (ch in chList) {
                    if (ch.accountID == accountID) {
                        presentSet[ch.challengeName] = ch as Object
                        listOfChallengeHistory.add(ch)
                    }

                }
                populateCurrent()
                loadNewChallenges()
            }

            override fun onError(e: Exception) {
                Log.e("ERROR_LOADING_CHALLENGES", e.message.toString())
                loadNewChallenges()
            }
        })
    }

    private fun populateCurrent() {
        var challengeList = mutableListOf<Challenges>()
        var newMap = HashMap<String, Challenges>()
        var isFinished = false
        for (l in listOfChallengeHistory) {
            if (l.isFinished) {
                isFinished = true
            }
            if (newMap.containsKey(l.challengeName)) {
                var challenge = newMap.get(l.challengeName)!!
                var availedMap = challenge.availedChallengeMap
                availedMap[l.valueIndex] = l.amount
                challenge.isFinished = isFinished
                challenge.availedChallengeMap = availedMap
                challenge.earned += l.amount.toString().toDouble()
                if (l.valueIndex == 0) {
                    challenge.startDatePaid = l.datePaid
                }
                challenge.remaining = challenge.amount - challenge.earned
                challenge.countMatch = challenge.countMatch + 1
                newMap[l.challengeName] = challenge
            } else {
                var challenge = Challenges()
                var availedMap = HashMap<Int, Int>()
                challenge.key = l.key
                challenge.isFinished = isFinished
                challenge.challengeName = l.challengeName
                challenge.amount =
                    Common().getChallengeAmountMap().get(l.challengeName).toString().toDouble()
                challenge.earned = l.amount.toString().toDouble()
                availedMap[l.valueIndex] = l.amount
                challenge.availedChallengeMap = availedMap
                challenge.remaining = challenge.amount - l.amount.toString().toDouble()
                challenge.countMatch = 1
                if (l.valueIndex == 0) {
                    challenge.startDatePaid = l.datePaid
                }

                newMap[l.challengeName] = challenge
            }
        }

        for (e in newMap.entries) {
            val challenges = e.value
            challengeList.add(challenges)
        }
        listOfChallenge = challengeList
        currentChallengesAdapter =
            CurrentChallengesAdapter(this, challengeList, object : ChallengeDetailListener {
                override fun onTextListener(position: Int) {
                    val challengeMap = Common().getChallengeMap() as MutableMap<String, Object>
                    val challengeTitleMap = Common().getChallengeTitleMap()
                    val ch = listOfChallenge.get(position)
                    var strName = ""
                    for (e in challengeTitleMap.entries) {
                        if (e.value == ch.challengeName) {
                            strName = e.key
                            break
                        }
                    }
                    if (challengeMap.containsKey(strName)) {
                        ch.values = challengeMap.get(strName) as Array<Int>
                    }
                    Log.e("CHALLENGES", ch.toString())
                    ContinueChallenge.start(
                        this@ChallengesMainActivity,
                        ch,
                        object : ChallengesIntf {
                            override fun OnProccessDone() {
                                loadChallenges()
                                recyclerNewChallenges.adapter = null
                                recyclerCurrentChallenges.adapter = null
                            }
                        }
                    )
                }

                override fun onClickFinishedChallenge(position: Int) {
                    val challengeMap = Common().getChallengeMap() as MutableMap<String, Object>
                    val challengeTitleMap = Common().getChallengeTitleMap()
                    val ch = listOfChallenge.get(position)
                    var strName = ""
                    for (e in challengeTitleMap.entries) {
                        if (e.value == ch.challengeName) {
                            strName = e.key
                            break
                        }
                    }
                    if (challengeMap.containsKey(strName)) {
                        ch.values = challengeMap.get(strName) as Array<Int>
                    }
                    ChallengesTotalEarned.start(this@ChallengesMainActivity, ch)
//                    if (ch.remaining != 0.0) {
//                        Log.e("TRIGGER", "TRUE")
//                    } else {
//
//                    }

                }

                override fun onRestartChallenge(position: Int) {
                    val rBuilder = AlertDialog.Builder(this@ChallengesMainActivity)
                    val rListener = object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            when (which) {
                                DialogInterface.BUTTON_NEGATIVE -> {
                                    pdLoading.show()
                                    val ch = listOfChallenge.get(position)
                                    var deleteIndex = 0
                                    for (lh in listOfChallengeHistory) {
                                        if (lh.challengeName.equals(ch.challengeName)) {
                                            db.deleteDataFromDB(
                                                "Challenge History",
                                                accountID,
                                                lh.key,
                                                object : FirebaseDatabaseCallback {
                                                    override fun onSuccessDelete() {
                                                        Log.e("SUCCESS", "DELETION")
                                                        if (deleteIndex == ch.countMatch - 1) {
                                                            pdLoading.dismiss()
                                                            Handler().postDelayed({
                                                                restartChallengeName =
                                                                    lh.challengeName
                                                                loadChallenges()
                                                                recyclerNewChallenges.adapter =
                                                                    null
                                                                recyclerCurrentChallenges.adapter =
                                                                    null


                                                            }, 500)
                                                            return
                                                        }
                                                        deleteIndex++
                                                    }

                                                    override fun onError(e: Exception) {
                                                        Log.e("ERROR_DELETE", e.message.toString())
                                                        if (deleteIndex == ch.countMatch - 1) {
                                                            pdLoading.dismiss()
                                                        }
                                                        deleteIndex++
                                                    }
                                                })

                                        }
                                    }

                                }
                                else -> {
                                    dialog.cancel()
                                }
                            }
                        }
                    }
                    rBuilder.setMessage("Are you sure you want to restart this challenge?")
                        .setNegativeButton("Yes", rListener)
                        .setPositiveButton("No", rListener)
                        .show()
                }
            })
        recyclerCurrentChallenges.layoutManager =
            object : LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        recyclerCurrentChallenges.adapter = currentChallengesAdapter
    }

    private fun loadNewChallenges() {

        var challengeMap = Common().getChallengeMap() as MutableMap<String, Object>
        var tmpChallengeMap = Common().getChallengeMap()
        var challengeTitleMap = Common().getChallengeTitleMap()
        //remove challengeMap element
        for (ch in tmpChallengeMap.entries) {
            val challengeName = challengeTitleMap.get(ch.key).toString()
            if (presentSet.containsKey(challengeName)) {
                challengeMap.remove(ch.key)
            }
        }
        var list = mutableListOf<Challenges>()
        var restartIndex = -1
        var currentValIndex = 0
        for (c in challengeMap.entries) {
            var challenge = Challenges()


            val challengeName = challengeTitleMap.get(c.key).toString()
            challenge.challengeName = challengeName
            challenge.values = c.value as Array<Int>
            if (restartChallengeName.equals(challengeName)) {
                restartIndex = currentValIndex
            }
            list.add(challenge)

            currentValIndex++
        }

        newChallengeAdapter = NewChallengesAdapter(this, list, object : NewChallengeIntf {
            override fun OnItemClickListener(position: Int) {
                ChallengesDetail1.start(
                    this@ChallengesMainActivity,
                    list.get(position),
                    object : ChallengesIntf {
                        override fun OnProccessDone() {
                            loadChallenges()
                            recyclerNewChallenges.adapter = null
                            recyclerCurrentChallenges.adapter = null
                        }
                    })
            }
        })
        recyclerNewChallenges.layoutManager =
            object : LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        recyclerNewChallenges.adapter = newChallengeAdapter

        if (restartIndex > -1 && recyclerNewChallenges.adapter != null) {
            Handler().postDelayed({
                recyclerNewChallenges.findViewHolderForAdapterPosition(restartIndex)!!.itemView.performClick()
                restartChallengeName = ""
            }, 300)
        }
    }


}