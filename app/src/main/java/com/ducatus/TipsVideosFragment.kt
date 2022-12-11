package com.ducatus

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.TipAdapter
import com.ducatus.common.AppResources
import com.ducatus.data.Tip
import com.ducatus.databinding.FragmentTipsVideosBinding
import com.ducatus.interfaces.TipInterface
import com.ducatus.viewmodel.SearchViewModel2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson

class TipsVideosFragment : Fragment(), TipInterface {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentTipsVideosBinding
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tipsVideosList: MutableList<Tip>
    private val searchViewModel2: SearchViewModel2 by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentTipsVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel2.searchInput.observe(viewLifecycleOwner) { name ->
            name.getContentIfNotHandled()?.let { content ->
                searchTipsVideosByTitle(content.lowercase())
            }
        }

        binding.tvTipsVideosSort.setOnClickListener {
            showPopup(it)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTipsVideos()

        binding.tvTipsVideosSort.visibility = View.VISIBLE
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    val fragmentManager = childFragmentManager
                    val newFragment = SearchItemDialog2Fragment()
                    newFragment.show(fragmentManager, "dialog")
                    true
                }
                else -> false
            }
        }
    }

    override fun viewItem(tip: Tip) {
        val intent = Intent(activity, TipsVideoDetailActivity::class.java)
        intent.putExtra("tip", Gson().toJson(tip))
        val options = ActivityOptions.makeSceneTransitionAnimation(activity)
        startActivity(intent, options.toBundle())
    }

    private fun loadTipsVideos() {
        val tipsList = AppResources().getTipsVideos() as MutableList<Tip>
        tipsList.sortByDescending { it.date }
        adaptTipsVideos(tipsList)
    }

    private fun searchTipsVideosByTitle(name: String) {
        loadTipsVideos()
        val newList = mutableListOf<Tip>()
        for (tip in tipsVideosList) {
            val title = tip.title.lowercase()
            if (title.startsWith(name) || title.endsWith(name)) {
                newList.add(tip)
            }
        }

        searchTipsVideoByAuthor(name, newList)
    }

    private fun searchTipsVideoByAuthor(name: String, newList: MutableList<Tip>) {
        for (tip in tipsVideosList) {
            val author = tip.author.lowercase()
            if (author.startsWith(name) || author.endsWith(name)) {
                newList.add(tip)
            }
        }

        if (newList.isNotEmpty()) {
            adaptTipsVideos(newList)
        }
        else {
            Snackbar
                .make(rootLayout, "No videos found with the name $name", Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun adaptTipsVideos(tipsList: MutableList<Tip>) {
        tipsVideosList = tipsList
        val tipAdapter = TipAdapter(tipsVideosList, this)
        binding.rvTipsVideos.adapter = tipAdapter
        binding.rvTipsVideos.layoutManager = LinearLayoutManager(activity)
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sortDatePublishedOldest -> {
                    tipsVideosList.sortBy { it.date }
                    adaptTipsVideos(tipsVideosList)
                    true
                }
                R.id.sortDatePublishedNewest -> {
                    tipsVideosList.sortByDescending { it.date }
                    adaptTipsVideos(tipsVideosList)
                    true
                }
                else -> false
            }
        }

        // menu to inflate
        popup.menuInflater.inflate(R.menu.sort_date_published_menu, popup.menu)
        popup.show()
    }
}