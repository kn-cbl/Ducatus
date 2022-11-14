package com.ducatus

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.CurrentChallengesAdapter
import com.ducatus.adapter.NewChallengesAdapter
import com.ducatus.common.Common
import com.ducatus.data.ChallengeHistory
import com.ducatus.data.Challenges
import com.ducatus.interfaces.ChallengesIntf
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.NewChallengeIntf
import com.ducatus.services.LocalFirebaseDatabase
import kotlinx.android.synthetic.main.challengesactivity_main_container.*
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class ChallengesMainActivity : AppCompatActivity() {

    lateinit var recyclerCurrentChallenges: RecyclerView
    lateinit var recyclerNewChallenges: RecyclerView
    lateinit var newChallengeAdapter: NewChallengesAdapter
    lateinit var currentChallengesAdapter: CurrentChallengesAdapter
    lateinit var listener: ChallengesIntf
    lateinit var presentSet: HashMap<String, Object>
    lateinit var db: LocalFirebaseDatabase
    lateinit var listOfChallengeHistory: MutableList<ChallengeHistory>
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
        listOfChallengeHistory = mutableListOf<ChallengeHistory>()
        recyclerCurrentChallenges = findViewById(R.id.recyclerCurrentChallenges)
        recyclerNewChallenges = findViewById(R.id.recyclerNewChallenges)
        accountID = SharedPreferences(this).accountId.toString()
        db = LocalFirebaseDatabase()
        loadChallenges()
    }

    private fun loadChallenges() {
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
        for (l in listOfChallengeHistory) {
            if (newMap.containsKey(l.challengeName)) {
                var challenge = newMap.get(l.challengeName)!!
                challenge.earned += l.amount.toString().toDouble()
                challenge.remaining = challenge.amount - challenge.earned
                newMap[l.challengeName] = challenge
            } else {
                var challenge = Challenges()
                challenge.challengeName = l.challengeName
                challenge.amount =
                    Common().getChallengeAmountMap().get(l.challengeName).toString().toDouble()
                challenge.earned = l.amount.toString().toDouble()
                challenge.remaining = challenge.amount - l.amount.toString().toDouble()
                newMap[l.challengeName] = challenge
            }
        }

        for (e in newMap.entries) {
            val challenges = e.value
            challengeList.add(challenges)
        }

        currentChallengesAdapter = CurrentChallengesAdapter(this, challengeList)
        recyclerCurrentChallenges.layoutManager =
            object : LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        recyclerCurrentChallenges.adapter = currentChallengesAdapter
    }

    private fun loadNewChallenges() {
        val challengeMap = Common().getChallengeMap() as MutableMap<String, Object>
        val tmpChallengeMap = Common().getChallengeMap()
        val challengeTitleMap = Common().getChallengeTitleMap()
        //remove challengeMap element
        for (ch in tmpChallengeMap.entries) {
            val challengeName = challengeTitleMap.get(ch.key).toString()
            if (presentSet.containsKey(challengeName)) {
                challengeMap.remove(ch.key)
            }
        }
        val challengeIndex = Common().getChallengeIndex()
        val set = HashMap<Int, Challenges>()
        val list = mutableListOf<Challenges>()
        var currentValIndex = 0
        for (c in challengeMap.entries) {
            var challenge = Challenges()


            val challengeName = challengeTitleMap.get(c.key).toString()
            challenge.challengeName = challengeName
            challenge.values = c.value as Array<Int>
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
    }


}