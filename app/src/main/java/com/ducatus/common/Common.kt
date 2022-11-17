package com.ducatus.common

import android.util.Log
import com.ducatus.data.ChallengeHistory
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.data.Tips
import java.util.*
import kotlin.collections.HashMap

class Common {
    fun toGoalsMap(goals: Goals): Map<String, Object> {
        var goalMap = hashMapOf<String, Object>()
        goalMap["key"] = goals.key as Object
        goalMap["accountID"] = goals.accountID as Object
        goalMap["goalDescription"] = goals.goalDescription as Object
        goalMap["targetDate"] = goals.targetDate as Object
        goalMap["percentage"] = goals.percentage as Object
        goalMap["earned"] = goals.earned as Object
        goalMap["remaining"] = goals.remaining as Object
        goalMap["goalAmount"] = goals.goalAmount as Object
        goalMap["notes"] = goals.notes as Object
        goalMap["color"] = goals.color as Object
        goalMap["colorName"] = goals.colorName as Object
        goalMap["icon"] = goals.icon as Object
        goalMap["status"] = goals.status as Object
        goalMap["dateGoalPaused"] = goals.dateGoalPaused as Object

        return goalMap
    }

    fun parseGoalMap(hash: Map<String, Object>): List<Goals> {
        var list = mutableListOf<Goals>()
        var hashData = hash.values
        for (goalData in hashData) {
            var data = goalData as Map<String, Object>
            var goal: Goals = Goals()
            goal.key = data["key"].toString()
            goal.accountID = data["accountID"].toString()
            goal.goalAmount = data["goalAmount"].toString().toDouble()
            goal.goalDescription = data["goalDescription"].toString()
            goal.targetDate = data["targetDate"].toString()
            goal.percentage = data["percentage"].toString().toDouble()
            goal.earned = data["earned"].toString().toDouble()
            goal.remaining = data["remaining"].toString().toDouble()
            goal.notes = data["notes"].toString()
            goal.colorName = data["colorName"].toString()
            goal.color = data["color"].toString().toInt()
            goal.icon = data["icon"].toString().toInt()
            goal.status = data["status"].toString().toInt()
            goal.dateGoalPaused = data["dateGoalPaused"].toString()
            list.add(goal)
        }
        return list
    }

    fun toGoalHistory(goalHistory: GoalHistory): Map<String, Object> {
        var goalMap = HashMap<String, Object>()
        goalMap["accountID"] = goalHistory.accountID as Object
        goalMap["goalkey"] = goalHistory.goalkey as Object
        goalMap["goalHistoryKey"] = goalHistory.goalHistoryKey as Object
        goalMap["datePaid"] = goalHistory.datePaid as Object
        goalMap["timePaid"] = goalHistory.timePaid as Object
        goalMap["amountPaid"] = goalHistory.amountPaid as Object

        return goalMap
    }

    fun parseGoalsHistory(hash: Map<String, Object>): List<GoalHistory> {
        var list = mutableListOf<GoalHistory>()
        var hashData = hash.values
        for (goalHistoryData in hashData) {
            var data = goalHistoryData as Map<String, Object>
            var goalHistory = GoalHistory()
            goalHistory.accountID = data["accountID"].toString()
            goalHistory.goalkey = data["goalkey"].toString()
            goalHistory.goalHistoryKey = data["goalHistoryKey"].toString()
            goalHistory.datePaid = data["datePaid"].toString()
            goalHistory.timePaid = data["timePaid"].toString()
            goalHistory.amountPaid = data["amountPaid"].toString().toDouble()
            list.add(goalHistory)

        }
        return list
    }

    fun toChallengeHistoryMap(challengeHistory: ChallengeHistory): Map<String, Object> {
        var map = HashMap<String, Object>()

        map["accountID"] = challengeHistory.accountID as Object
        map["key"] = challengeHistory.key as Object
        map["challengeName"] = challengeHistory.challengeName as Object
        map["datePaid"] = challengeHistory.datePaid as Object
        map["timePaid"] = challengeHistory.timePaid as Object
        map["amount"] = challengeHistory.amount as Object
        map["valueIndex"] = challengeHistory.valueIndex as Object
        map["isFinished"] = challengeHistory.isFinished as Object
        return map
    }

    fun parseChallengeHistory(hash: Map<String, Object>): List<ChallengeHistory> {
        var list = mutableListOf<ChallengeHistory>()
        var hashData = hash.values
        for (challengeHistoryData in hashData) {
            var data = challengeHistoryData as Map<String, Object>
            var challengeHistory = ChallengeHistory()
            challengeHistory.accountID = data["accountID"].toString()
            challengeHistory.key = data["key"].toString()
            challengeHistory.challengeName = data["challengeName"].toString()
            challengeHistory.datePaid = data["datePaid"].toString()
            challengeHistory.timePaid = data["timePaid"].toString()
            challengeHistory.amount = data["amount"].toString().toInt()
            challengeHistory.isFinished = data["isFinished"].toString().toBoolean()
            challengeHistory.valueIndex = data["valueIndex"].toString().toInt()
            list.add(challengeHistory)
        }

        Log.e("PARSE", list.toString())
        return list
    }

    fun getTipsMap(): List<Tips> {
        var list = mutableListOf<Tips>()
        var tips = Tips(
            "https://www.realsimple.com/work-life/money/money-planning/tips-for-first-time-budgeting",
            "2022-08-09",
            "4 Tips For First-time Budgeting",
            "Hiranmayi Srinivasan",
            "https://raw.githubusercontent.com/MakMoinee/makmoinee.github.io/main/tips.png",
        )
        list.add(tips)

        tips = Tips(
            "https://moneytamer.com/budgeting-tips-for-beginners/",
            "2020-01-29",
            "Budgeting Tips For Beginners: How To Start A Budget That Works",
            "Steffa Mantilla, CFEI",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips2.png?raw=true",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips2Author.png?raw=true"
        )
        list.add(tips)

        tips = Tips(
            "https://www.prulifeuk.com.ph/en/explore-pulse/health-financial-wellness/50-30-20-budgeting-hack/",
            "",
            "Is The 50-30-20 Budgeting Hack Right For You?",
            "Prolife UK",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips3.jpg?raw=true"
        )
        list.add(tips)

        tips = Tips(
            "https://www.mymoneycoach.ca/blog/how-to-save-money-on-low-income",
            "",
            "4 Tips to Save Money on Low Income",
            "Kevin Sun",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips4.png?raw=true"
        )
        list.add(tips)

        tips = Tips(
            "https://mint.intuit.com/blog/planning/money-101-27-financial-tips-to-live-by/",
            "2022-05-04",
            "Financial Advice: 12 Personal Finance Tips",
            "Matthew Amster-Burton",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips5.png?raw=true"
        )
        list.add(tips)

        tips = Tips(
            "https://www.investopedia.com/articles/younginvestors/08/eight-tips.asp",
            "2022-05-14",
            "8 Financial Tips for Young Adults",
            "AMY FONTINELLE",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips6.png?raw=true"
        )
        list.add(tips)

        tips = Tips(
            "https://www.sunlife.com.ph/en/life-goals/grow-your-money/how-to-achieve-financial-stability-in-changing-times/",
            "2022-08-24",
            "How to achieve financial stability in changing times",
            "Sunlife",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips7.png?raw=true"
        )
        list.add(tips)

        return list
    }

    fun getTipsVideoMap(): List<Tips> {
        var list = mutableListOf<Tips>()

        var tips = Tips(
            "https://www.youtube.com/watch?v=WBf1Q53tv7I",
//            "https://rr7---sn-bavcx-jxcz.googlevideo.com/videoplayback?expire=1668116422&ei=ZhttY-u4E72S1d8Pi_-AgA8&ip=180.190.160.2&id=o-AHCeeIO3q5Mc2FLKONlL_nLXu2rG1r_eB5h3u-Uxzstl&itag=134&aitags=133%2C134%2C135%2C136%2C137%2C160%2C242%2C243%2C244%2C247%2C248%2C278&source=youtube&requiressl=yes&mh=GO&mm=31%2C29&mn=sn-bavcx-jxcz%2Csn-bavcx-hoaek&ms=au%2Crdu&mv=m&mvi=7&pl=22&initcwndbps=691250&spc=SFxXNvj0kbJgNxTibj9_Xc960IFQ5vY&vprv=1&mime=video%2Fmp4&ns=zLf_kKTlh-ev_2r67s_WC8EJ&gir=yes&clen=6043039&otfp=1&dur=139.166&lmt=1606118838495583&mt=1668094392&fvip=2&keepalive=yes&fexp=24001373%2C24007246&c=WEB&txp=6216222&n=lYy9aWslzrQEcvYFobY&sparams=expire%2Cei%2Cip%2Cid%2Caitags%2Csource%2Crequiressl%2Cspc%2Cvprv%2Cmime%2Cns%2Cgir%2Cclen%2Cotfp%2Cdur%2Clmt&sig=AOq0QJ8wRgIhAJ-eqqRRU0Bqv_nw5F9POZMk0TEMf9bgzEZtS4CLDcZkAiEA772DjFIwM7Nhcxnni7Z1Al5N10ioC2hu2_6nsat9ZTc%3D&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRAIgPlH1kUKCnWY4gv74aPAZ798v-IqNmHMhOhboogoxG_wCIBbMbL1WBk0v5e9otFec6Vg69Jsx17a5kWHMUG5-ACKJ",
            "2021-10-08",
            "Budgeting 101 Guide for Pinoy",
            "Pinay Investor",
            "https://i3.ytimg.com/vi/CFrhSBwPJwU/maxresdefault.jpg"
        )
        list.add(tips)

        tips = Tips(
            "https://www.youtube.com/watch?v=gct3D8v2cSo",
            "2021-10-02",
            "Financial Planning for Beginners",
            "Pinay Investor",
            "https://i3.ytimg.com/vi/gct3D8v2cSo/maxresdefault.jpg"
        )
        list.add(tips)

        tips = Tips(
            "https://www.youtube.com/watch?v=OYY_FXec1jY",
            "2021-10-05",
            "Paano Gumawa ng Financial Plan | Building Your Financial Home | Financial Planning 101",
            "Pinay Investor",
            "https://i3.ytimg.com/vi/OYY_FXec1jY/maxresdefault.jpg"
        )
        list.add(tips)

        tips = Tips(
            "https://www.youtube.com/watch?v=OyYL4C7nwvU",
            "2021-12-29",
            "How to Monitor Your Budget | Budget Tips | Free Budget Planner 2021",
            "Budget Pinay",
            "https://i3.ytimg.com/vi/OyYL4C7nwvU/maxresdefault.jpg"
        )
        list.add(tips)

        tips = Tips(
            "https://www.youtube.com/watch?v=7_oy3AJI23s",
            "2020-11-30",
            "How To Create Monthly Budget Plan | Budgeting Tips",
            "Budget Pinay",
            "https://i3.ytimg.com/vi/7_oy3AJI23s/maxresdefault.jpg"
        )
        list.add(tips)

        tips = Tips(
            "https://www.youtube.com/watch?v=WBf1Q53tv7I",
            "2020-11-10",
            "Top 7 Grocery Budget Tips | Tipid Tips",
            "Budget Pinay",
            "https://i3.ytimg.com/vi/WBf1Q53tv7I/maxresdefault.jpg"
        )
        list.add(tips)

        return list
    }

    fun getYoutubeBody(watchPath: String, watchID: String): Map<String, Object> {
        val map = HashMap<String, Object>()
        val contextMap = HashMap<String, Object>()
        val clientMap = HashMap<String, Object>()
        clientMap["hl"] = "en" as Object
        clientMap["clientName"] = "WEB" as Object
        clientMap["clientVersion"] = "2.20210721.00.00" as Object
        val mainAppWebInfo = HashMap<String, Object>()
        mainAppWebInfo["graftUrl"] = watchPath as Object
        clientMap["mainAppWebInfo"] = mainAppWebInfo as Object

        contextMap["client"] = clientMap as Object
        map["context"] = contextMap as Object
        map["videoId"] = watchID as Object
        return map
    }

    fun getChallengeMap(): Map<String, Object> {
        val map = HashMap<String, Object>()

        val sevenDaysArr = Array(7) { 0 }
        sevenDaysArr[0] = 31
        sevenDaysArr[1] = 53
        sevenDaysArr[2] = 58
        sevenDaysArr[3] = 10
        sevenDaysArr[4] = 14
        sevenDaysArr[5] = 22
        sevenDaysArr[6] = 62
        map["two_fifty_pesos_savings_in_seven_days"] = sevenDaysArr as Object

        val sevenDaysForOneHundredArr = Array(7) { 0 }
        sevenDaysForOneHundredArr[0] = 11
        sevenDaysForOneHundredArr[1] = 6
        sevenDaysForOneHundredArr[2] = 21
        sevenDaysForOneHundredArr[3] = 7
        sevenDaysForOneHundredArr[4] = 15
        sevenDaysForOneHundredArr[5] = 5
        sevenDaysForOneHundredArr[6] = 35
        map["one_hundred_pesos_savings_in_seven_days"] = sevenDaysForOneHundredArr as Object


        val fourteenDaysForFiveHundredArr = Array(14) { 0 }
        fourteenDaysForFiveHundredArr[0] = 86
        fourteenDaysForFiveHundredArr[1] = 74
        fourteenDaysForFiveHundredArr[2] = 13
        fourteenDaysForFiveHundredArr[3] = 141
        fourteenDaysForFiveHundredArr[4] = 11
        fourteenDaysForFiveHundredArr[5] = 20
        fourteenDaysForFiveHundredArr[6] = 10
        fourteenDaysForFiveHundredArr[7] = 12
        fourteenDaysForFiveHundredArr[8] = 21
        fourteenDaysForFiveHundredArr[9] = 15
        fourteenDaysForFiveHundredArr[10] = 30
        fourteenDaysForFiveHundredArr[11] = 28
        fourteenDaysForFiveHundredArr[12] = 25
        fourteenDaysForFiveHundredArr[13] = 14
        map["five_hundred_pesos_savings_in_fourteen_days"] = fourteenDaysForFiveHundredArr as Object

        val fourteenDaysForOneThousandArr = Array(14) { 0 }
        fourteenDaysForOneThousandArr[0] = 96
        fourteenDaysForOneThousandArr[1] = 25
        fourteenDaysForOneThousandArr[2] = 81
        fourteenDaysForOneThousandArr[3] = 126
        fourteenDaysForOneThousandArr[4] = 92
        fourteenDaysForOneThousandArr[5] = 24
        fourteenDaysForOneThousandArr[6] = 87
        fourteenDaysForOneThousandArr[7] = 10
        fourteenDaysForOneThousandArr[8] = 88
        fourteenDaysForOneThousandArr[9] = 58
        fourteenDaysForOneThousandArr[10] = 96
        fourteenDaysForOneThousandArr[11] = 48
        fourteenDaysForOneThousandArr[12] = 134
        fourteenDaysForOneThousandArr[13] = 35
        map["one_thousand_pesos_savings_in_fourteen_days"] = fourteenDaysForOneThousandArr as Object


        val thirtyDaysForFiveThousandArr = Array(30) { 0 }
        thirtyDaysForFiveThousandArr[0] = 12
        thirtyDaysForFiveThousandArr[1] = 222
        thirtyDaysForFiveThousandArr[2] = 155
        thirtyDaysForFiveThousandArr[3] = 141
        thirtyDaysForFiveThousandArr[4] = 243
        thirtyDaysForFiveThousandArr[5] = 286
        thirtyDaysForFiveThousandArr[6] = 162
        thirtyDaysForFiveThousandArr[7] = 196
        thirtyDaysForFiveThousandArr[8] = 136
        thirtyDaysForFiveThousandArr[9] = 128
        thirtyDaysForFiveThousandArr[10] = 232
        thirtyDaysForFiveThousandArr[11] = 285
        thirtyDaysForFiveThousandArr[12] = 144
        thirtyDaysForFiveThousandArr[13] = 292
        thirtyDaysForFiveThousandArr[14] = 236
        thirtyDaysForFiveThousandArr[15] = 216
        thirtyDaysForFiveThousandArr[16] = 203
        thirtyDaysForFiveThousandArr[17] = 16
        thirtyDaysForFiveThousandArr[18] = 192
        thirtyDaysForFiveThousandArr[19] = 11
        thirtyDaysForFiveThousandArr[20] = 88
        thirtyDaysForFiveThousandArr[21] = 134
        thirtyDaysForFiveThousandArr[22] = 262
        thirtyDaysForFiveThousandArr[23] = 14
        thirtyDaysForFiveThousandArr[24] = 183
        thirtyDaysForFiveThousandArr[25] = 165
        thirtyDaysForFiveThousandArr[26] = 224
        thirtyDaysForFiveThousandArr[27] = 215
        thirtyDaysForFiveThousandArr[28] = 197
        thirtyDaysForFiveThousandArr[29] = 10
        map["five_thousand_pesos_savings_in_thirty_days"] = thirtyDaysForFiveThousandArr as Object


        val thirtyDaysForThreeThousandArr = Array(30) { 0 }
        thirtyDaysForThreeThousandArr[0] = 60
        thirtyDaysForThreeThousandArr[1] = 70
        thirtyDaysForThreeThousandArr[2] = 85
        thirtyDaysForThreeThousandArr[3] = 90
        thirtyDaysForThreeThousandArr[4] = 140
        thirtyDaysForThreeThousandArr[5] = 100
        thirtyDaysForThreeThousandArr[6] = 120
        thirtyDaysForThreeThousandArr[7] = 95
        thirtyDaysForThreeThousandArr[8] = 160
        thirtyDaysForThreeThousandArr[9] = 75
        thirtyDaysForThreeThousandArr[10] = 120
        thirtyDaysForThreeThousandArr[11] = 85
        thirtyDaysForThreeThousandArr[12] = 65
        thirtyDaysForThreeThousandArr[13] = 70
        thirtyDaysForThreeThousandArr[14] = 115
        thirtyDaysForThreeThousandArr[15] = 90
        thirtyDaysForThreeThousandArr[16] = 75
        thirtyDaysForThreeThousandArr[17] = 130
        thirtyDaysForThreeThousandArr[18] = 80
        thirtyDaysForThreeThousandArr[19] = 55
        thirtyDaysForThreeThousandArr[20] = 90
        thirtyDaysForThreeThousandArr[21] = 95
        thirtyDaysForThreeThousandArr[22] = 120
        thirtyDaysForThreeThousandArr[23] = 85
        thirtyDaysForThreeThousandArr[24] = 190
        thirtyDaysForThreeThousandArr[25] = 175
        thirtyDaysForThreeThousandArr[26] = 80
        thirtyDaysForThreeThousandArr[27] = 75
        thirtyDaysForThreeThousandArr[28] = 70
        thirtyDaysForThreeThousandArr[29] = 140
        map["three_thousand_pesos_savings_in_thirty_days"] = thirtyDaysForThreeThousandArr as Object


        val sixtyDaysForTenThousandArr = Array(60) { 0 }
        sixtyDaysForTenThousandArr[0] = 118
        sixtyDaysForTenThousandArr[1] = 124
        sixtyDaysForTenThousandArr[2] = 103
        sixtyDaysForTenThousandArr[3] = 139
        sixtyDaysForTenThousandArr[4] = 405
        sixtyDaysForTenThousandArr[5] = 104
        sixtyDaysForTenThousandArr[6] = 111
        sixtyDaysForTenThousandArr[7] = 103
        sixtyDaysForTenThousandArr[8] = 148
        sixtyDaysForTenThousandArr[9] = 108
        sixtyDaysForTenThousandArr[10] = 129
        sixtyDaysForTenThousandArr[11] = 110
        sixtyDaysForTenThousandArr[12] = 113
        sixtyDaysForTenThousandArr[13] = 151
        sixtyDaysForTenThousandArr[14] = 107
        sixtyDaysForTenThousandArr[15] = 134
        sixtyDaysForTenThousandArr[16] = 117
        sixtyDaysForTenThousandArr[17] = 137
        sixtyDaysForTenThousandArr[18] = 484
        sixtyDaysForTenThousandArr[19] = 464
        sixtyDaysForTenThousandArr[20] = 116
        sixtyDaysForTenThousandArr[21] = 105
        sixtyDaysForTenThousandArr[22] = 136
        sixtyDaysForTenThousandArr[23] = 121
        sixtyDaysForTenThousandArr[24] = 140
        sixtyDaysForTenThousandArr[25] = 131
        sixtyDaysForTenThousandArr[26] = 126
        sixtyDaysForTenThousandArr[27] = 127
        sixtyDaysForTenThousandArr[28] = 146
        sixtyDaysForTenThousandArr[29] = 135
        sixtyDaysForTenThousandArr[30] = 143
        sixtyDaysForTenThousandArr[31] = 994
        sixtyDaysForTenThousandArr[32] = 142
        sixtyDaysForTenThousandArr[33] = 132
        sixtyDaysForTenThousandArr[34] = 133
        sixtyDaysForTenThousandArr[35] = 149
        sixtyDaysForTenThousandArr[36] = 125
        sixtyDaysForTenThousandArr[37] = 115
        sixtyDaysForTenThousandArr[38] = 193
        sixtyDaysForTenThousandArr[39] = 145
        sixtyDaysForTenThousandArr[40] = 138
        sixtyDaysForTenThousandArr[41] = 112
        sixtyDaysForTenThousandArr[42] = 141
        sixtyDaysForTenThousandArr[23] = 52
        sixtyDaysForTenThousandArr[44] = 119
        sixtyDaysForTenThousandArr[45] = 100
        sixtyDaysForTenThousandArr[46] = 226
        sixtyDaysForTenThousandArr[47] = 109
        sixtyDaysForTenThousandArr[48] = 122
        sixtyDaysForTenThousandArr[49] = 102
        sixtyDaysForTenThousandArr[50] = 101
        sixtyDaysForTenThousandArr[51] = 120
        sixtyDaysForTenThousandArr[52] = 160
        sixtyDaysForTenThousandArr[53] = 123
        sixtyDaysForTenThousandArr[54] = 284
        sixtyDaysForTenThousandArr[55] = 114
        sixtyDaysForTenThousandArr[56] = 128
        sixtyDaysForTenThousandArr[57] = 106
        sixtyDaysForTenThousandArr[58] = 144
        sixtyDaysForTenThousandArr[59] = 409
        map["ten_thousand_pesos_savings_in_sixty_days"] = sixtyDaysForTenThousandArr as Object

        val ninetyDaysForTwentyThousandArr = Array(90) { 0 }
        ninetyDaysForTwentyThousandArr[0] = 123
        ninetyDaysForTwentyThousandArr[1] = 128
        ninetyDaysForTwentyThousandArr[2] = 159
        ninetyDaysForTwentyThousandArr[3] = 428
        ninetyDaysForTwentyThousandArr[4] = 160
        ninetyDaysForTwentyThousandArr[5] = 175
        ninetyDaysForTwentyThousandArr[6] = 793
        ninetyDaysForTwentyThousandArr[7] = 102
        ninetyDaysForTwentyThousandArr[8] = 394
        ninetyDaysForTwentyThousandArr[9] = 311
        ninetyDaysForTwentyThousandArr[10] = 140
        ninetyDaysForTwentyThousandArr[11] = 156
        ninetyDaysForTwentyThousandArr[12] = 137
        ninetyDaysForTwentyThousandArr[13] = 131
        ninetyDaysForTwentyThousandArr[14] = 101
        ninetyDaysForTwentyThousandArr[15] = 841
        ninetyDaysForTwentyThousandArr[16] = 169
        ninetyDaysForTwentyThousandArr[17] = 145
        ninetyDaysForTwentyThousandArr[18] = 149
        ninetyDaysForTwentyThousandArr[19] = 136
        ninetyDaysForTwentyThousandArr[20] = 165
        ninetyDaysForTwentyThousandArr[21] = 811
        ninetyDaysForTwentyThousandArr[22] = 111
        ninetyDaysForTwentyThousandArr[23] = 132
        ninetyDaysForTwentyThousandArr[24] = 152
        ninetyDaysForTwentyThousandArr[25] = 221
        ninetyDaysForTwentyThousandArr[26] = 122
        ninetyDaysForTwentyThousandArr[27] = 117
        ninetyDaysForTwentyThousandArr[28] = 143
        ninetyDaysForTwentyThousandArr[29] = 148
        ninetyDaysForTwentyThousandArr[30] = 161
        ninetyDaysForTwentyThousandArr[31] = 776
        ninetyDaysForTwentyThousandArr[32] = 125
        ninetyDaysForTwentyThousandArr[33] = 126
        ninetyDaysForTwentyThousandArr[34] = 138
        ninetyDaysForTwentyThousandArr[35] = 153
        ninetyDaysForTwentyThousandArr[36] = 135
        ninetyDaysForTwentyThousandArr[37] = 124
        ninetyDaysForTwentyThousandArr[38] = 162
        ninetyDaysForTwentyThousandArr[39] = 964
        ninetyDaysForTwentyThousandArr[40] = 110
        ninetyDaysForTwentyThousandArr[41] = 105
        ninetyDaysForTwentyThousandArr[42] = 166
        ninetyDaysForTwentyThousandArr[23] = 113
        ninetyDaysForTwentyThousandArr[44] = 171
        ninetyDaysForTwentyThousandArr[45] = 112
        ninetyDaysForTwentyThousandArr[46] = 141
        ninetyDaysForTwentyThousandArr[47] = 163
        ninetyDaysForTwentyThousandArr[48] = 130
        ninetyDaysForTwentyThousandArr[49] = 168
        ninetyDaysForTwentyThousandArr[50] = 120
        ninetyDaysForTwentyThousandArr[51] = 921
        ninetyDaysForTwentyThousandArr[52] = 146
        ninetyDaysForTwentyThousandArr[53] = 144
        ninetyDaysForTwentyThousandArr[54] = 150
        ninetyDaysForTwentyThousandArr[55] = 109
        ninetyDaysForTwentyThousandArr[56] = 139
        ninetyDaysForTwentyThousandArr[57] = 129
        ninetyDaysForTwentyThousandArr[58] = 134
        ninetyDaysForTwentyThousandArr[59] = 147
        ninetyDaysForTwentyThousandArr[60] = 185
        ninetyDaysForTwentyThousandArr[61] = 945
        ninetyDaysForTwentyThousandArr[62] = 133
        ninetyDaysForTwentyThousandArr[63] = 142
        ninetyDaysForTwentyThousandArr[64] = 155
        ninetyDaysForTwentyThousandArr[65] = 151
        ninetyDaysForTwentyThousandArr[66] = 472
        ninetyDaysForTwentyThousandArr[67] = 127
        ninetyDaysForTwentyThousandArr[68] = 115
        ninetyDaysForTwentyThousandArr[69] = 210
        ninetyDaysForTwentyThousandArr[70] = 121
        ninetyDaysForTwentyThousandArr[71] = 470
        ninetyDaysForTwentyThousandArr[72] = 104
        ninetyDaysForTwentyThousandArr[73] = 477
        ninetyDaysForTwentyThousandArr[74] = 118
        ninetyDaysForTwentyThousandArr[75] = 108
        ninetyDaysForTwentyThousandArr[76] = 107
        ninetyDaysForTwentyThousandArr[77] = 157
        ninetyDaysForTwentyThousandArr[78] = 364
        ninetyDaysForTwentyThousandArr[79] = 164
        ninetyDaysForTwentyThousandArr[80] = 114
        ninetyDaysForTwentyThousandArr[81] = 100
        ninetyDaysForTwentyThousandArr[82] = 106
        ninetyDaysForTwentyThousandArr[83] = 119
        ninetyDaysForTwentyThousandArr[84] = 154
        ninetyDaysForTwentyThousandArr[85] = 116
        ninetyDaysForTwentyThousandArr[86] = 103
        ninetyDaysForTwentyThousandArr[87] = 651
        ninetyDaysForTwentyThousandArr[88] = 172
        ninetyDaysForTwentyThousandArr[89] = 158
        map["twenty_thousand_pesos_savings_in_ninety_days"] =
            ninetyDaysForTwentyThousandArr as Object

        return map
    }

    fun getChallengeTitleMap(): Map<String, String> {
        val map = HashMap<String, String>()
        map["two_fifty_pesos_savings_in_seven_days"] = "P50 in 7 Days"
        map["one_hundred_pesos_savings_in_seven_days"] = "P100 in 7 Days"
        map["five_hundred_pesos_savings_in_fourteen_days"] = "P500 in 14 Days"
        map["one_thousand_pesos_savings_in_fourteen_days"] = "P1000 in 14 Days"
        map["five_thousand_pesos_savings_in_thirty_days"] = "P5000 in 30 Days"
        map["three_thousand_pesos_savings_in_thirty_days"] = "P3000 in 30 Days"
        map["ten_thousand_pesos_savings_in_sixty_days"] = "P10000 in 60 Days"
        map["twenty_thousand_pesos_savings_in_ninety_days"] = "P20000 in 90 Days"
        return map
    }

    fun getChallengeAmountMap(): Map<String, Int> {
        val map = HashMap<String, Int>()
        map["P50 in 7 Days"] = 50
        map["P100 in 7 Days"] = 100
        map["P500 in 14 Days"] = 500
        map["P1000 in 14 Days"] = 1000
        map["P5000 in 30 Days"] = 5000
        map["P3000 in 30 Days"] = 3000
        map["P10000 in 60 Days"] = 10000
        map["P20000 in 90 Days"] = 20000
        return map
    }

    fun getChallengeDaysMap(): Map<String, Int> {
        val map = HashMap<String, Int>()
        map["P50 in 7 Days"] = 6
        map["P100 in 7 Days"] = 6
        map["P500 in 14 Days"] = 13
        map["P1000 in 14 Days"] = 13
        map["P5000 in 30 Days"] = 29
        map["P3000 in 30 Days"] = 29
        map["P10000 in 60 Days"] = 59
        map["P20000 in 90 Days"] = 89
        return map
    }

    fun getChallengeIndex(): Map<String, Int> {
        val map = HashMap<String, Int>()
        map["two_fifty_pesos_savings_in_seven_days"] = 0
        map["one_hundred_pesos_savings_in_seven_days"] = 1
        map["five_hundred_pesos_savings_in_fourteen_days"] = 2
        map["one_thousand_pesos_savings_in_fourteen_days"] = 3
        map["five_thousand_pesos_savings_in_thirty_days"] = 4
        map["three_thousand_pesos_savings_in_thirty_days"] = 5
        map["ten_thousand_pesos_savings_in_sixty_days"] = 6
        map["twenty_thousand_pesos_savings_in_ninety_days"] = 7
        return map
    }
}

