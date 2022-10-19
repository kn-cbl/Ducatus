package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.databinding.FragmentCategoriesBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class CategoriesFragment : Fragment(), CategoryInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoriesBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        toolbar = activity.findViewById(R.id.tbCategories)
        toolbar.title = getString(R.string.categories)
        toolbar.menu.clear()

        binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.ibAddCategory.setOnClickListener {
            toolbar.title = "Add Category"
            val action = CategoriesFragmentDirections.actionCategoriesFragmentToCategoryAddFragment()
            findNavController().navigate(action)
        }
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun showPopup(view: View, position: Int, categoryId: String) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.optionEdit -> {
                    toolbar.title = getString(R.string.edit_category)
                    val action = CategoriesFragmentDirections.actionCategoriesFragmentToCategoryEditFragment(categoryId)
                    findNavController().navigate(action)
                    true
                }
                R.id.optionDelete-> {
                    confirmDelete(categoryId, position)
                    true
                }
                else -> false
            }
        }
        popup.menuInflater.inflate(R.menu.edit_options_2_menu, popup.menu)
        popup.show()
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            databaseReference = database.getReference("categories").child(firebaseUser.uid).child(currentAccountId)
            loadCategories()
        }
        else {
            sessionExpired()
        }
    }

    private fun loadCategories() {
        showProgressDialog()
        categoryAdapter = CategoryAdapter(mutableListOf(), this)
        binding.rvCategories.adapter = categoryAdapter
        binding.rvCategories.layoutManager = LinearLayoutManager(activity)

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                val categories = mutableListOf<Category>()
                for (child in snapshot.children) {
                    val category = child.getValue<Category>()
                    if (category != null) {
                        categories.add(category)
                    }
                }

                // sort categories
                categories.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.category_name.toString() })
                for (item in categories) {
                    categoryAdapter.addCategory(item)
                }

                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, "Unable to load data, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { loadCategories() }
                    .show()
            }

        if (categoryAdapter.itemCount >= 20) {
            binding.ibAddCategory.visibility = View.GONE
        }
    }

    private fun confirmDelete(categoryId: String, position: Int) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.delete_category_mark))
            .setMessage(resources.getString(R.string.delete_category_confirm))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteCategory(categoryId, position) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteCategory(categoryId: String, position: Int) {
        databaseReference = databaseReference.child(categoryId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                categoryAdapter.removeCategory(position)
                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, "Unable to delete category, ${it.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { deleteCategory(categoryId, position) }
                    .show()
            }
    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                try {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbCategories.visibility = View.VISIBLE
        binding.rvCategories.visibility = View.GONE
        binding.ibAddCategory.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbCategories.visibility = View.INVISIBLE
        binding.rvCategories.visibility = View.VISIBLE
        binding.ibAddCategory.visibility = View.VISIBLE
    }
}