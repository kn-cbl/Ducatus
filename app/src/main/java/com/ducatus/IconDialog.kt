package com.ducatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment


class IconDialog : DialogFragment(){

    public var selectedIcon: Int = 0
    lateinit var displayIcon : ImageButton

    val iconName = ArrayList<String>()
    val iconResource = ArrayList<Int>()
    
    fun addIcon(name:String, icon:Int){
        if(name == null || icon == null) return

        iconName.add(name)
        iconResource.add(icon)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var rootView = inflater.inflate(R.layout.icon_dialog, container, false)

        addIcon("", R.drawable.ic_baseline_globe_24)
        addIcon("", R.drawable.ic_baseline_car_24)
        addIcon("", R.drawable.ic_baseline_plane_24)
        addIcon("", R.drawable.ic_baseline_keyboard_24)
        addIcon("", R.drawable.ic_baseline_bank_24)
        addIcon("", R.drawable.ic_baseline_dining_24)
        addIcon("", R.drawable.ic_baseline_tool_24)
        addIcon("", R.drawable.ic_baseline_umbrella_24)
        addIcon("", R.drawable.ic_baseline_savings_24)
        addIcon("", R.drawable.ic_baseline_design_24)

        addIcon("", R.drawable.ic_baseline_school_24)
        addIcon("", R.drawable.ic_baseline_build_24)
        addIcon("", R.drawable.ic_baseline_music_24)
        addIcon("", R.drawable.ic_baseline_book_24)
        addIcon("", R.drawable.ic_baseline_art_24)
        addIcon("", R.drawable.ic_baseline_business_24)
        addIcon("", R.drawable.ic_baseline_cable_24)
        addIcon("", R.drawable.ic_baseline_birthday_24)
        addIcon("", R.drawable.ic_baseline_calendar_24)
        addIcon("", R.drawable.ic_baseline_camera_24)

        addIcon("", R.drawable.ic_baseline_campaign_24)
        addIcon("", R.drawable.ic_baseline_gift_24)
        addIcon("", R.drawable.ic_baseline_carpenter_24)
        addIcon("", R.drawable.ic_baseline_celebration_24)
        addIcon("", R.drawable.ic_baseline_social_24)
        addIcon("", R.drawable.ic_baseline_date_24)
        addIcon("", R.drawable.ic_baseline_child_24)
        addIcon("", R.drawable.ic_baseline_baby_24)
        addIcon("", R.drawable.ic_baseline_church_24)
        addIcon("", R.drawable.ic_baseline_sanitize_24)

        addIcon("", R.drawable.ic_baseline_cleaning_24)
        addIcon("", R.drawable.ic_baseline_cloudstorage_24)
        addIcon("", R.drawable.ic_baseline_coffee_24)
        addIcon("", R.drawable.ic_baseline_bookmark_24)
        addIcon("", R.drawable.ic_baseline_colorlens_24)
        addIcon("", R.drawable.ic_baseline_transportation_24)
        addIcon("", R.drawable.ic_baseline_planting_24)
        addIcon("", R.drawable.ic_baseline_computer_24)
        addIcon("", R.drawable.ic_baseline_airport_24)
        addIcon("", R.drawable.ic_baseline_construction_24)

        addIcon("", R.drawable.ic_baseline_motor_24)
        addIcon("", R.drawable.ic_baseline_jewelry_24)
        addIcon("", R.drawable.ic_baseline_payment_24)
        addIcon("", R.drawable.ic_baseline_bus_24)
        addIcon("", R.drawable.ic_baseline_volunteer_24)
        addIcon("", R.drawable.ic_baseline_people_24)
        addIcon("", R.drawable.ic_baseline_draw_24)
        addIcon("", R.drawable.ic_baseline_shop_24)
        addIcon("", R.drawable.ic_baseline_eco_24)
        addIcon("", R.drawable.ic_baseline_bike_24)

        addIcon("", R.drawable.ic_baseline_electronics_24)
        addIcon("", R.drawable.ic_baseline_health_24)
        addIcon("", R.drawable.ic_baseline_event_24)
        addIcon("", R.drawable.ic_baseline_food_24)
        addIcon("", R.drawable.ic_baseline_fitness_24)
        addIcon("", R.drawable.ic_baseline_hike_24)
        addIcon("", R.drawable.ic_baseline_flag_24)
        addIcon("", R.drawable.ic_baseline_game_24)
        addIcon("", R.drawable.ic_baseline_star_24)
        addIcon("", R.drawable.ic_baseline_home_24)

        addIcon("", R.drawable.ic_baseline_sports_24)
        addIcon("", R.drawable.ic_baseline_phone_24)
        addIcon("", R.drawable.ic_baseline_passion_24)
        addIcon("", R.drawable.ic_baseline_leisure_24)
        addIcon("", R.drawable.ic_baseline_pets_24)
        addIcon("", R.drawable.ic_baseline_toys_24)
        addIcon("", R.drawable.ic_baseline_store_24)
        addIcon("", R.drawable.ic_baseline_travel_24)
        addIcon("", R.drawable.ic_baseline_outdoor_24)
        addIcon("", R.drawable.ic_baseline_beauty_24)
        addIcon("", R.drawable.ic_baseline_night_24)
        addIcon("", R.drawable.ic_baseline_trophy_24)

        var gridView = rootView.findViewById<GridView>(R.id.icon_grid)
        var adapter = object :ArrayAdapter<String>(
            gridView.context,
            0,
            iconName
        ){

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position)

                val view = LayoutInflater.from(getContext()).inflate(R.layout.icon_item, parent, false);

                val textView = view.findViewById<TextView>(R.id.icon_text)
                textView.text = iconName[position]

                val imageButton = view.findViewById<ImageButton>(R.id.icon_image)
                imageButton.setImageResource(iconResource[position])

                imageButton.setOnClickListener {
                    selectedIcon = iconResource[position]
                    displayIcon.setImageResource(iconResource[position])
                    dismiss()
                }

                return view
            }


        }

//        Toast.makeText(context, adapter.count.toString(), Toast.LENGTH_SHORT).show()


        gridView.adapter = adapter


        return rootView
    }



}