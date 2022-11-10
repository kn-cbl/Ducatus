package com.ducatus.common

import android.util.Log
import com.ducatus.AppResources
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.data.Tips
import kotlinx.serialization.json.JsonArray
import org.json.JSONObject
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
}

