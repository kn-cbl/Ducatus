package com.ducatus

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.ducatus.common.AppResources
import com.ducatus.databinding.FragmentIconDialogBinding
import com.ducatus.viewmodel.IconViewModel

class IconDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentIconDialogBinding
    private val viewModel: IconViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentIconDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadIcons()
    }

    private fun loadIcons() {
        val icons = AppResources().getIcons()
        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.item_icon, icons) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(activity).inflate(R.layout.item_icon, parent, false)
                val image = view.findViewById<ImageView>(R.id.ivItemIcon)

                val icon = resources.getIdentifier(
                    icons[position],
                    "drawable",
                    activity.packageName
                )

                image?.setImageResource(icon)
                image?.setColorFilter(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.dark_gray,
                        null
                    )
                )

                image?.setOnClickListener {
                    viewModel.setIcon(icons[position])
                    dismiss()
                }

                return view
            }
        }

        binding.gvIcon.adapter = adapter
    }
}