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


        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)
        addIcon("Palm", R.drawable.ic_sample_img_goal)
        addIcon("Car", R.drawable.ic_car)
        addIcon("Loan", R.drawable.ic_loan_icon)

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