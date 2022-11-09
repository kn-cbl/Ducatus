package com.ducatus

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.ActiveGoalIntf
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.GoalDetailIntf
import com.ducatus.services.LocalFirebaseDatabase
import kotlinx.android.synthetic.main.edit_goal.*
import kotlinx.android.synthetic.main.fragment_goal_active_container.*
import java.lang.Exception
import java.time.LocalDate


class EditGoal : AppCompatActivity() {
    val listItemColor = ArrayList<String>()
    val listColor = ArrayList<Int>()
    lateinit var spinner: Spinner
    lateinit var result: TextView
    lateinit var adapterColor: ArrayAdapter<String>
    lateinit var context: Context
    lateinit var mGoal: Goals
    lateinit var editGoalName: EditText
    lateinit var editTargetAmount: EditText
    lateinit var db: LocalFirebaseDatabase
    lateinit var eventListener: GoalDetailIntf
    lateinit var txtSaved: TextView
    lateinit var editTargetDate: EditText
    lateinit var editNote: EditText
    private var currentColor: Int = 0
    private var currentColorName: String = ""
    lateinit var database: LocalFirebaseDatabase
    lateinit var pb: ProgressDialog
    lateinit var gvIcon: GridView
    var flag: Boolean = false
    lateinit var iconAlertDialog: AlertDialog
    private var currentIcon: Int = 0
    lateinit var icons: ImageButton
    lateinit var ghList: List<GoalHistory>
    var isFromReach: Boolean = false
    lateinit var iconsDropdown: ImageView


    companion object {
        lateinit var goal: Goals
        lateinit var detailIntf: GoalDetailIntf
        var isFromPause: Boolean = false
        var isFromDetailPassedFromReach: Boolean = false
        lateinit var goalHistoryList: List<GoalHistory>

        fun start(
            mContext: Context,
            g: Goals,
            d: GoalDetailIntf,
            flag: Boolean,
            isFromReach: Boolean,
            gh: List<GoalHistory>
        ) {
            goal = g
            detailIntf = d
            isFromPause = flag
            goalHistoryList = gh
            isFromDetailPassedFromReach = isFromReach
            val intent: Intent = Intent(mContext, EditGoal::class.java)
            mContext.startActivity(intent)
        }
    }

    fun addColor(name: String, color: Int) {
        if (name != null && color != null) {
            listItemColor.add(name)
            listColor.add(color)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flag = EditGoal.isFromPause
        isFromReach = EditGoal.isFromDetailPassedFromReach
        mGoal = EditGoal.goal
        if (isFromReach) {
            setContentView(R.layout.edit_goal_reached_passed)
        } else {
            if (flag) {
                setContentView(R.layout.edit_goal_pause)
            } else {
                setContentView(R.layout.edit_goal)
            }
        }


        context = this
        addColor("color_one", getColor(R.color.color_one))
        addColor("color_two", getColor(R.color.color_two))
        addColor("color_three", getColor(R.color.color_three))
        addColor("color_four", getColor(R.color.color_four))
        addColor("color_five", getColor(R.color.color_five))
        addColor("color_six", getColor(R.color.color_six))
        addColor("color_seven", getColor(R.color.color_seven))
        addColor("color_eight", getColor(R.color.color_eight))
        addColor("color_nine", getColor(R.color.color_nine))
        addColor("color_ten", getColor(R.color.color_ten))

        addColor("color_eleven", getColor(R.color.color_eleven))
        addColor("color_twelve", getColor(R.color.color_twelve))
        addColor("color_thirteen", getColor(R.color.color_thirteen))
        addColor("color_fourteen", getColor(R.color.color_fourteen))
        addColor("color_fifteen", getColor(R.color.color_fifteen))
        addColor("color_sixteen", getColor(R.color.color_sixteen))
        addColor("color_seventeen", getColor(R.color.color_seventeen))
        addColor("color_eighteen", getColor(R.color.color_eighteen))
        addColor("color_nineteen", getColor(R.color.color_nineteen))
        addColor("color_twenty", getColor(R.color.color_twenty))

        addColor("color_twenty_one", getColor(R.color.color_twenty_one))
        addColor("color_twenty_two", getColor(R.color.color_twenty_two))
        addColor("color_twenty_three", getColor(R.color.color_twenty_three))
        addColor("color_twenty_four", getColor(R.color.color_twenty_four))
        addColor("color_twenty_five", getColor(R.color.color_twenty_five))

        spinner = findViewById(R.id.spinner_color)

        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            context,
            R.layout.spinner_item,
            R.id.txt_bundle,
            listItemColor
        ) {
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = getView(position, convertView, parent)
                val color = view.findViewById<View>(R.id.viewHelperItem)
                val db: GradientDrawable = color.background as GradientDrawable
                db.setColor(listColor[position])
                currentColor = mGoal.color
                db.cornerRadius = 20f
                color.background = db
                return view
            }

        }

        spinner.adapter = adapter


        spinner.onItemSelectedListener = object
            : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
//                Toast.makeText(context, listItemColor[position], Toast.LENGTH_SHORT).show()
//                spinner.emptyView = spinner.selectedView
//                spinner.setBackgroundColor(listColor[position])

                val sv = spinner.selectedView
                val svv = sv.findViewById<View>(R.id.viewHelperItem)
                val db: GradientDrawable = GradientDrawable()
                db.setColor(listColor[position])
                currentColor = listColor[position]
                currentColorName = listItemColor[position]
                db.cornerRadius = 20f
                svv.background = db
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // another interface callback
            }
        }

        spinner.setSelection(listItemColor.indexOf(mGoal.colorName))

        icons = findViewById<ImageButton>(R.id.spinner_icon)
        iconsDropdown = findViewById<ImageView>(R.id.icon_dropdown)
        initViews()
        icons.setOnClickListener {
            val iBuilder = AlertDialog.Builder(this)
            val iView =
                LayoutInflater.from(this).inflate(R.layout.fragment_icon_dialog, null, false)
            initIconViews(iView)
            loadIcons(iView)
            iBuilder.setView(iView)

            iconAlertDialog = iBuilder.create()
            iconAlertDialog.show()

        }
        iconsDropdown.setOnClickListener {
            val iBuilder = AlertDialog.Builder(this)
            val iView =
                LayoutInflater.from(this).inflate(R.layout.fragment_icon_dialog, null, false)
            initIconViews(iView)
            loadIcons(iView)
            iBuilder.setView(iView)

            iconAlertDialog = iBuilder.create()
            iconAlertDialog.show()

        }

        icons.setImageResource(goal.icon)
        currentIcon = goal.icon


        initOtherListeners()
    }

    private fun initIconViews(iView: View) {
        gvIcon = iView.findViewById(R.id.gvIcon)
    }

    private fun loadIcons(iView: View) {
        try {
            val localIcons = AppResources().getIcons()
            val adapter = object : ArrayAdapter<String>(this, R.layout.item_icon, localIcons) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val iiView = LayoutInflater.from(applicationContext)
                        .inflate(R.layout.item_icon, null, false)
                    val image: ImageView = iiView.findViewById(R.id.ivItemIcon)

                    val icon = resources.getIdentifier(
                        localIcons[position],
                        "drawable",
                        packageName
                    )

                    image.setImageResource(icon)
                    image.setColorFilter(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.darker_gray,
                            null
                        )
                    )

                    image.setOnClickListener {
                        currentIcon = icon
                        iconAlertDialog.dismiss()
                        icons.setImageResource(icon)
                        return@setOnClickListener
                    }

                    return iiView
                }
            }
            gvIcon.adapter = adapter
        } catch (e: Exception) {
            Log.e("ERROR_LOADING_ICONS", e.message.toString())
            iconAlertDialog.dismiss()
        }
    }


    private fun initOtherListeners() {
        editGoal_toolbar.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })

        editGoal_toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.editGoalPause -> {
                    val alertDialog = AlertDialog.Builder(this)
                    val dlistener = object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            when (which) {
                                DialogInterface.BUTTON_NEGATIVE -> {
                                    pb.show()
                                    var entities = LocalEntities()
                                    entities.goals = mGoal
                                    database.updateToDb(
                                        entities,
                                        "Goals Pause",
                                        object : FirebaseDatabaseCallback {
                                            override fun onError(e: Exception) {
                                                pb.dismiss()
                                                Log.e("ERROR_PAUSING", e.message.toString())
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Failed to Pause Goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                            }

                                            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                                                TODO("Not yet implemented")
                                            }

                                            override fun onSuccessInsert(key: String) {
                                                pb.dismiss()
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Successfully Paused Goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                eventListener.onSuccessUpdate()
                                                finish()
                                            }

                                            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                                                TODO("Not yet implemented")
                                            }
                                        })
                                }
                                DialogInterface.BUTTON_POSITIVE -> {
                                    pb.dismiss()
                                }
                            }
                        }
                    }
                    alertDialog.setMessage("Are You Sure You Want To Proceed Pausing This Goal?")
                        .setNegativeButton("Yes", dlistener)
                        .setPositiveButton("No", dlistener)
                        .setCancelable(false)
                        .show()
                    true
                }
                R.id.editGoalPlay -> {

                    pb.show()
                    val alertDialog = AlertDialog.Builder(this)
                    val dlistener = object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            when (which) {
                                DialogInterface.BUTTON_NEGATIVE -> {
                                    var entities = LocalEntities()
                                    entities.goals = mGoal
                                    database.updateToDb(
                                        entities,
                                        "Goals",
                                        object : FirebaseDatabaseCallback {
                                            override fun onError(e: Exception) {
                                                pb.dismiss()
                                                Log.e("ERROR_PAUSING", e.message.toString())
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Failed to Pause Goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                            }

                                            override fun onSuccessListOfGoalHistory(
                                                goalHistoryList: List<GoalHistory>
                                            ) {
                                                TODO("Not yet implemented")
                                            }

                                            override fun onSuccessInsert(key: String) {
                                                pb.dismiss()
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Successfully Resumed Goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                eventListener.onSuccessUpdate()
                                                finish()
                                            }

                                            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                                                TODO("Not yet implemented")
                                            }
                                        })
                                }
                                DialogInterface.BUTTON_POSITIVE -> {
                                    pb.dismiss()
                                }
                            }
                        }
                    }
                    alertDialog.setMessage("You are about to resume this goal, Do you want to proceed?")
                        .setNegativeButton("Yes", dlistener)
                        .setPositiveButton("No", dlistener)
                        .setCancelable(false)
                        .show()
                    true

                }
                R.id.editGoalDelete -> {
                    val alertDialog = AlertDialog.Builder(this)
                    val dialogIntf = object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, id: Int) {
                            when (id) {
                                DialogInterface.BUTTON_NEGATIVE -> {
                                    db.deleteDataFromDB(
                                        "Goals",
                                        mGoal.accountID,
                                        mGoal.key,
                                        object : FirebaseDatabaseCallback {
                                            override fun onError(e: Exception) {
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Failed to delete goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                                                TODO("Not yet implemented")
                                            }

                                            override fun onSuccessInsert(key: String) {
                                                for (gh in ghList) {
                                                    deleteGoalHistory(
                                                        "Goal History",
                                                        mGoal.accountID,
                                                        gh.goalHistoryKey
                                                    )
                                                }
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Successfully Deleted Goal",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                eventListener.deleteSubmitted()
                                                finish()
                                            }

                                            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                                                TODO("Not yet implemented")
                                            }
                                        })
                                }
                            }
                        }
                    }
                    alertDialog.setMessage("Are You Sure You Want To Delete This Goal?")
                        .setPositiveButton("No", dialogIntf)
                        .setNegativeButton("Yes", dialogIntf)
                        .setCancelable(false)
                        .show()
                    true
                }
                R.id.iconCheck -> {
                    var isProceed = true
                    if (isFromReach) {
                        if (editTargetDate.text.toString().equals("")) {
                            Toast.makeText(
                                applicationContext,
                                "Target Date must be filled",
                                Toast.LENGTH_SHORT
                            ).show()
                            isProceed = false
                        } else {
                            var strArr = editTargetDate.text.toString().split("/")
                            var convertedDate = strArr[2] + "-" + strArr[0] + "-" + strArr[1]
                            if (LocalDate.parse(convertedDate).isBefore(LocalDate.now())) {
                                Toast.makeText(
                                    applicationContext,
                                    "Target Date must not be before current date",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isProceed = false
                            }else{
                                isProceed = true
                            }
                        }

                    }

                    if (isProceed) {
                        if (editGoalName.text.toString().equals("") ||
                            editTargetAmount.text.toString().equals("") ||
                            editTargetDate.text.toString().equals("") ||
                            editNote.text.toString().equals("")
                        ) {
                            Toast.makeText(
                                this,
                                "Please Don't Leave Empty Fields",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            pb.show()
                            var goals: Goals = Goals()
                            goals.accountID = mGoal.accountID
                            goals.goalDescription = editGoalName.text.toString()
                            var amount = editTargetAmount.text.toString().toDouble()
                            goals.goalAmount = amount
                            var save = txtSaved.text.toString().toDouble()
                            goals.earned = txtSaved.text.toString().toDouble()
                            goals.targetDate = editTargetDate.text.toString()
                            goals.color = currentColor;
                            goals.notes = editNote.text.toString()
                            goals.percentage = save / amount * 100
                            goals.remaining = amount - save
                            goals.key = mGoal.key
                            goals.colorName = currentColorName
                            goals.icon = currentIcon

                            var entities = LocalEntities()
                            entities.goals = goals

                            db.updateToDb(entities, "Goals", object : FirebaseDatabaseCallback {
                                override fun onSuccessInsert(key: String) {
                                    pb.dismiss()
                                    Toast.makeText(
                                        applicationContext,
                                        "Successfull Updated Goals",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    eventListener.onSuccessUpdate()
                                    finish()

                                }

                                override fun onError(e: Exception) {
                                    Log.e("ERROR_UPDATE", e.message.toString())
                                    pb.dismiss()
                                    Toast.makeText(
                                        applicationContext,
                                        "Failed to update goal",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                }

                                override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                                    TODO("Not yet implemented")
                                }

                                override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                                    TODO("Not yet implemented")
                                }
                            })
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        })
    }

    private fun deleteGoalHistory(s: String, accountID: String, goalHistoryKey: String) {
        db.deleteDataFromDB(s, accountID, goalHistoryKey, object : FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                Log.w("SUCCESS_DELETE", "Successfully Delete Goal History: " + key)
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                TODO("Not yet implemented")
            }

            override fun onError(e: Exception) {
                Log.e("ERROR_DELETE", e.message.toString())
            }
        })
    }

    private fun initViews() {
        mGoal = EditGoal.goal
        eventListener = EditGoal.detailIntf
        flag = EditGoal.isFromPause
        ghList = EditGoal.goalHistoryList
        isFromReach = EditGoal.isFromDetailPassedFromReach
        pb = ProgressDialog(this)
        pb.setMessage("Sending Request ...")
        pb.setCancelable(false)
        editGoalName = findViewById(R.id.editTextName_editGoal)
        editTargetAmount = findViewById(R.id.edittargetAmount_editGoal)
        txtSaved = findViewById(R.id.textSavedAlready_editGoal)
        editGoalName = findViewById(R.id.editTextName_editGoal)
        editTargetDate = findViewById(R.id.editDate_editGoal)
        editNote = findViewById(R.id.editTextNotes_editGoal)
        database = LocalFirebaseDatabase()

        editGoalName.setText(mGoal.goalDescription)
        editTargetAmount.setText(mGoal.goalAmount.toString())
        txtSaved.setText(mGoal.earned.toString())
        editTargetDate.setText(mGoal.targetDate)
        editNote.setText(mGoal.notes)

        if (flag) {
            editGoalName.isEnabled = false
            editTargetAmount.isEnabled = false
            txtSaved.isEnabled = false
            editTargetDate.isEnabled = false
            editNote.isEnabled = false
            spinner.isEnabled = false
            icons.isEnabled = false
            iconsDropdown.isEnabled = false
        } else {
            editGoalName.isEnabled = true
            editTargetAmount.isEnabled = true
            txtSaved.isEnabled = true
            editTargetDate.isEnabled = true
            editNote.isEnabled = true
            spinner.isEnabled = true
            icons.isEnabled = true
            iconsDropdown.isEnabled = true
        }


        if (isFromReach) {
            editGoalName.isEnabled = false
            editTargetAmount.isEnabled = false
            txtSaved.isEnabled = false
            editTargetDate.isEnabled = true
            editNote.isEnabled = false
            spinner.isEnabled = false
            icons.isEnabled = false
            iconsDropdown.isEnabled = false
        }



        db = LocalFirebaseDatabase()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}


private fun <T> ArrayAdapter<T>.getDropDownView() {
    TODO("Not yet implemented")
}