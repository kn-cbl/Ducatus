package com.ducatus

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ducatus.data.Goals
import com.ducatus.services.LocalFirebaseDatabase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.new_goal.*
import kotlinx.coroutines.selects.select


class NewGoal : AppCompatActivity() {
    val listItemColor = ArrayList<String>()
    val listColor = ArrayList<Int>()
    lateinit var spinner: Spinner
    lateinit var result: TextView
    lateinit var adapterColor: ArrayAdapter<String>
    lateinit var context: Context
    lateinit var accountID: String
    private val db: LocalFirebaseDatabase = LocalFirebaseDatabase()


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
        newGoal_toolbar.setOnClickListener(View.OnClickListener {
            onBackPressed()
        })
        newGoal_toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.done -> {
                    Toast.makeText(this, "Saving ..", Toast.LENGTH_SHORT).show()
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
                db.cornerRadius = 20f
                svv.background = db
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // another interface callback
            }
        }


        val icons = findViewById<ImageButton>(R.id.spinner_icon)
        val iconsDropdown = findViewById<ImageView>(R.id.icon_dropdown)

        icons.setOnClickListener {
            // do something when the corky2 is clicked
            val dialog = IconDialogFragment()
//            dialog.displayIcon = icons
            dialog.show(supportFragmentManager, "Icon Dialog")
        }
        iconsDropdown.setOnClickListener {
            val dialog = IconDialogFragment()
//            dialog.displayIcon = icons
            dialog.show(supportFragmentManager, "Icon Dialog")
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.done) {

//            val goals: Goals = Goals(accountID,)
            Toast.makeText(this, "Saving ...", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }


}


private fun <T> ArrayAdapter<T>.getDropDownView() {
    TODO("Not yet implemented")
}

