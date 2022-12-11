package com.ducatus

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.ducatus.common.AppResources
import com.ducatus.databinding.FragmentColorDialogBinding
import com.ducatus.viewmodel.ColorViewModel

class ColorDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentColorDialogBinding
    private val viewModel: ColorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentColorDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadColors()
    }

    private fun loadColors() {
        val colors = AppResources().getColors()
        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.item_color, colors) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(activity).inflate(R.layout.item_color, parent, false)
                val color = view.findViewById<View>(R.id.viewItemColor)
                val gradientDrawable = GradientDrawable()

                val iconColor = resources.getIdentifier(
                    colors[position],
                    "color",
                    activity.packageName
                )

                gradientDrawable.setColor(activity.getColor(iconColor))
                gradientDrawable.cornerRadius = 100f
                color.background = gradientDrawable

                color.setOnClickListener {
                    viewModel.setColor(colors[position])
                    dismiss()
                }

                return view
            }
        }

        binding.gvColor.adapter = adapter
    }
}