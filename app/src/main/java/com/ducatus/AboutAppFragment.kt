package com.ducatus

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.ducatus.databinding.FragmentAboutAppBinding
import com.google.android.material.appbar.MaterialToolbar

class AboutAppFragment : Fragment() {
//    private lateinit var activity: Activity
    private lateinit var binding: FragmentAboutAppBinding
//    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        activity = requireActivity()
//        toolbar = requireActivity().findViewById(R.id.tbHome)

        binding = FragmentAboutAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        toolbar.setNavigationIcon(R.drawable.ic_back)
//        toolbar.setNavigationOnClickListener {
//            toolbar.setTitle(R.string.settings)
//            val action = AboutAppFragmentDirections.actionAboutAppFragmentToSettingsFragment()
//            findNavController().navigate(action)
//        }
    }
}