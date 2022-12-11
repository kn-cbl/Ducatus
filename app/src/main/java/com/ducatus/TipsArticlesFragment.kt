package com.ducatus

import android.app.Activity
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
import com.ducatus.databinding.FragmentTipsArticlesBinding
import com.ducatus.interfaces.TipInterface
import com.ducatus.viewmodel.SearchViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson

class TipsArticlesFragment : Fragment(), TipInterface {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentTipsArticlesBinding
    private lateinit var rootLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tipsArticlesList: MutableList<Tip>
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.dlHome)
        toolbar = activity.findViewById(R.id.tbHome)

        binding = FragmentTipsArticlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel.searchInput.observe(viewLifecycleOwner) { name ->
            name.getContentIfNotHandled()?.let { content ->
                searchTipsArticlesByTitle(content.lowercase())
            }
        }

        binding.tvTipsArticlesSort.visibility = View.VISIBLE
        binding.tvTipsArticlesSort.setOnClickListener {
            showPopup(it)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTipsArticles()

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    val fragmentManager = childFragmentManager
                    val newFragment = SearchItemDialogFragment()
                    newFragment.show(fragmentManager, "dialog")
                    true
                }
                else -> false
            }
        }
    }

    override fun viewItem(tip: Tip) {
        val intent = Intent(activity, TipsArticleDetailActivity::class.java)
        intent.putExtra("tip", Gson().toJson(tip))
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadTipsArticles() {
        val tipsList = AppResources().getTipsArticles() as MutableList<Tip>
        tipsList.sortByDescending { it.date }
        adaptTipsArticles(tipsList)
    }

    private fun searchTipsArticlesByTitle(name: String) {
        loadTipsArticles()
        val newList = mutableListOf<Tip>()
        for (tip in tipsArticlesList) {
            val title = tip.title.lowercase()
            if (title.startsWith(name) || title.endsWith(name)) {
                newList.add(tip)
            }
        }

        searchTipsArticleByAuthor(name, newList)
    }

    private fun searchTipsArticleByAuthor(name: String, newList: MutableList<Tip>) {
        for (tip in tipsArticlesList) {
            val author = tip.author.lowercase()
            if (author.startsWith(name) || author.endsWith(name)) {
                newList.add(tip)
            }
        }

        if (newList.isNotEmpty()) {
            adaptTipsArticles(newList)
        }
        else {
            Snackbar
                .make(rootLayout, "No articles found with the name $name", Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun adaptTipsArticles(tipsList: MutableList<Tip>) {
        tipsArticlesList = tipsList
        val tipAdapter = TipAdapter(tipsList, this)
        binding.rvTipsArticles.adapter = tipAdapter
        binding.rvTipsArticles.layoutManager = LinearLayoutManager(activity)
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sortDatePublishedOldest -> {
                    tipsArticlesList.sortBy { it.date }
                    adaptTipsArticles(tipsArticlesList)
                    true
                }
                R.id.sortDatePublishedNewest -> {
                    tipsArticlesList.sortByDescending { it.date }
                    adaptTipsArticles(tipsArticlesList)
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