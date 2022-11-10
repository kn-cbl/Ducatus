package com.ducatus

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducatus.adapter.TipsAdapter
import com.ducatus.common.Common
import com.ducatus.data.Tips
import com.ducatus.interfaces.TipsListener

class TipsArticlesFragment : Fragment() {

    lateinit var parentView: View
    lateinit var recycler: RecyclerView
    lateinit var adapter: TipsAdapter
    lateinit var tipsList: List<Tips>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.fragment_tips_articles_container, container, false)
        parentView = mView
        initViews(mView)
        return mView
    }

    private fun initViews(mView: View) {
        recycler = mView.findViewById(R.id.recycler)
        tipsList = Common().getTipsMap()
        adapter = TipsAdapter(requireContext(), tipsList, object : TipsListener {
            override fun OnItemClickListener(position: Int) {
                TipsDetailArticle.start(requireContext(), requireActivity(), tipsList.get(position))
            }
        })
        recycler.layoutManager =
            GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        recycler.adapter = adapter
    }
}