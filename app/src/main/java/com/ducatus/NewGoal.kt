package com.ducatus

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.GridIconsAdapter
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import com.ducatus.data.LocalEntities
import com.ducatus.interfaces.FirebaseDatabaseCallback
import com.ducatus.interfaces.GoalIntf
import com.ducatus.services.LocalFirebaseDatabase
import com.ducatus.viewmodel.IconViewModel
import kotlinx.android.synthetic.main.new_goal.*
import java.time.LocalDate
import java.util.regex.Pattern


class NewGoal : AppCompatActivity() {
    val listItemColor = ArrayList<String>()
    val listColor = ArrayList<Int>()
    lateinit var spinner: Spinner
    lateinit var result: TextView
    lateinit var adapterColor: ArrayAdapter<String>
    lateinit var context: Context
    lateinit var accountID: String
    lateinit var icons: ImageButton
    private val db: LocalFirebaseDatabase = LocalFirebaseDatabase()
    private lateinit var goalName: EditText
    private lateinit var targetAmount: EditText
    private lateinit var saved: EditText
    private lateinit var targetDate: EditText
    private lateinit var notes: EditText
    private var currentColor: Int = 0
    private var currentColorName: String = ""
    lateinit var listener: GoalIntf
    lateinit var iconAlertDialog: AlertDialog
    lateinit var localIcons: List<String>
    lateinit var gvIcon: GridView
    private var currentIcon: Int = 0
    lateinit var pdLoading: ProgressDialog


    companion object {
        lateinit var goalIntf: GoalIntf

        fun start(mContext: Context, mGoalIntf: GoalIntf) {
            NewGoal.goalIntf = mGoalIntf
            val intent: Intent = Intent(mContext, NewGoal::class.java)
            mContext.startActivity(intent)
        }

        fun getInterface(): GoalIntf {
            return NewGoal.goalIntf
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
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        setContentView(R.layout.new_goal)
        initViews()
        newGoal_toolbar.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })
        newGoal_toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.done -> {
                    if (goalName.text.toString().equals("") ||
                        targetAmount.text.toString().equals("") ||
                        saved.text.toString().equals("") ||
                        targetDate.text.toString().equals("") ||
                        notes.text.toString().equals("")
                    ) {
                        Toast.makeText(this, "Please Don't Leave Empty Fields", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        var tgd = targetDate.text.toString().replace("/", "")
                        if (tgd.length < 8) {
                            Toast.makeText(
                                this,
                                "Please Fill Valid Target Date",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            var targetDateStr = ""
                            var tmpStr = ""
                            var tgdCount = 0
                            var tgdCharArr = tgd.toCharArray()
                            for (i in tgdCharArr.indices) {
                                tgdCount++
                                tmpStr += tgdCharArr.get(i).toString()
                                if (tgdCount == 2) {
                                    targetDateStr = tmpStr + "/"
                                    tmpStr = ""
                                } else if (tgdCount == 4) {
                                    targetDateStr += tmpStr + "/"
                                    tmpStr = ""
                                } else if (tgdCount == 8) {
                                    targetDateStr += tmpStr
                                    tmpStr = ""
                                }

                            }
                            pdLoading.show()
                            var goals: Goals = Goals()
                            goals.accountID = accountID.toString()
                            goals.goalDescription = goalName.text.toString()
                            var amount = targetAmount.text.toString().toDouble()
                            goals.goalAmount = amount
                            var save = saved.text.toString().toDouble()
                            goals.earned = saved.text.toString().toDouble()
                            goals.targetDate = targetDateStr
                            goals.color = currentColor;
                            goals.notes = notes.text.toString()
                            goals.percentage = save / amount * 100
                            goals.remaining = amount - save
                            goals.colorName = currentColorName
                            if (currentIcon == 0) {
                                val iconTmp = resources.getIdentifier(
                                    "ic_baseline_home_24",
                                    "drawable",
                                    packageName
                                )
                                currentIcon = iconTmp
                            }
                            goals.icon = currentIcon



                            Log.w("accountID", accountID.toString())

                            var entities = LocalEntities()
                            entities.goals = goals

                            val callback: FirebaseDatabaseCallback =
                                object : FirebaseDatabaseCallback {
                                    override fun onSuccessInsert(key: String) {
                                        if (saved.text.toString().toDouble() == 0.0) {
                                            pdLoading.hide()
                                            Toast.makeText(
                                                applicationContext,
                                                "Successfully Added Goal",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish()
                                            goalIntf.PressBack()
                                        } else {
                                            var goalHistory = GoalHistory()
                                            goalHistory.goalkey = key
                                            goalHistory.accountID = goals.accountID
                                            goalHistory.amountPaid =
                                                saved.text.toString().toDouble()
                                            goalHistory.datePaid = LocalDate.now().toString()
                                            entities.goalHistory = goalHistory
                                            saveGoalHistory(entities)
                                        }


                                    }

                                    override fun onError(e: Exception) {
                                        pdLoading.hide()
                                        Toast.makeText(
                                            applicationContext,
                                            "Failed to Add Goal",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        Log.e("ADDING_GOAL_ERR", e.message.toString())
                                    }

                                    override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                                        TODO("Not yet implemented")
                                    }

                                    override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                                        TODO("Not yet implemented")
                                    }
                                }

                            db.writeToDb(entities, "Goals", callback)
                        }

                    }

                    true
                }
                else -> false
            }
        }

        val sharedPreferences = SharedPreferences(this)
        accountID = sharedPreferences.accountId!!
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
                currentColor = listColor[position]
                currentColorName = listItemColor[position]
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


        icons = findViewById<ImageButton>(R.id.spinner_icon)
        val iconsDropdown = findViewById<ImageView>(R.id.icon_dropdown)

        icons.setOnClickListener {
            // do something when the corky2 is clicked
//            val dialog = IconDialogFragment()
//            dialog.displayIcon = icons
//            dialog.show(supportFragmentManager, "Icon Dialog")
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


    }

    private fun saveGoalHistory(entities: LocalEntities) {
        db.writeToDb(entities, "Goal History", object : FirebaseDatabaseCallback {
            override fun onSuccessInsert(key: String) {
                pdLoading.hide()
                Toast.makeText(
                    applicationContext,
                    "Successfully Added Goal",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                goalIntf.PressBack()
            }

            override fun onError(e: java.lang.Exception) {
                pdLoading.hide()
                Log.e("ERROR_GOAL_DETAIL", e.message.toString())
                Toast.makeText(
                    applicationContext,
                    "Failed to Save Amount Info",
                    Toast.LENGTH_SHORT
                )
            }

            override fun onSuccessListOfGoals(goalsList: List<Goals>) {
                TODO("Not yet implemented")
            }

            override fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun initIconViews(iView: View) {
        gvIcon = iView.findViewById(R.id.gvIcon)
    }

    private fun loadIcons(iView: View) {
        try {
            localIcons = AppResources().getIcons()
            var first = true
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
                    currentIcon = icon
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

    private fun initViews() {
        pdLoading = ProgressDialog(this)
        pdLoading.setMessage("Sending Request ...")
        goalName = findViewById(R.id.editTextName_newGoal)
        targetAmount = findViewById(R.id.edittargetAmount_newGoal)
        saved = findViewById(R.id.textSavedAlready_newGoal)
        targetDate = findViewById(R.id.editDate_newGGoal)
        notes = findViewById(R.id.editTextNotes_newGoal)
        listener = NewGoal.goalIntf
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}


private fun <T> ArrayAdapter<T>.getDropDownView() {
    TODO("Not yet implemented")
}

