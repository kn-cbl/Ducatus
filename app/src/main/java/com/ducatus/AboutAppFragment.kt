package com.ducatus

import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentAboutAppBinding

class AboutAppFragment : Fragment() {
    private lateinit var actionBar: androidx.appcompat.app.ActionBar
    private lateinit var activity: Activity
    private lateinit var binding: FragmentAboutAppBinding
    private lateinit var toolbar: Toolbar

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        actionBar = (requireActivity() as AppCompatActivity).supportActionBar!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentAboutAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = activity.findViewById(R.id.tbHome)
//        actionBar.setHomeAsUpIndicator(R.drawable.ic_back)
//        actionBar.setHomeButtonEnabled(true)
    }
}