package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.data.Category
import com.ducatus.data.Subcategory
import com.ducatus.databinding.FragmentCategoryEditBinding
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

class CategoryEditFragment : Fragment(), SubcategoryInterface {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryEditBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var categoryReference: DatabaseReference
    private lateinit var subcategoryReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selectedCategoryListener: ValueEventListener
    private lateinit var subcategoriesListener: ChildEventListener
    private lateinit var subcategoryAdapter: SubcategoryAdapter
    private lateinit var currentCategoryColor: String
    private lateinit var currentCategoryIcon: String
    private lateinit var currentCategoryName: String
    private lateinit var currentCategoryNature: String
    private val args: CategoryEditFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = requireActivity()
        auth = Firebase.auth
        if (auth.currentUser != null) {
            firebaseUser = auth.currentUser!!
        }
        else {
            sessionExpired()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootLayout = activity.findViewById(R.id.llCategories)
        toolbar = activity.findViewById(R.id.tbCategories)
        toolbar.title = getString(R.string.edit_category)
        toolbar.menu.clear()

        binding = FragmentCategoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        showProgressDialog()

        sharedPreferences = SharedPreferences(activity)
        val currentAccountId = sharedPreferences.accountId.toString()

        setSelectedCategoryListener()
        setSubcategoriesListener()

        database = Firebase.database
        categoryReference = database.getReference("categories").child(firebaseUser.uid).child(currentAccountId).child(args.categoryId)
        categoryReference.addValueEventListener(selectedCategoryListener)

        subcategoryReference = database.getReference("subcategories").child(firebaseUser.uid).child(currentAccountId).child(args.categoryId)
        subcategoryReference.addChildEventListener(subcategoriesListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibEditCategoryIcon.setOnClickListener {
            val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditIconDialogFragment(args.categoryId, currentCategoryColor, currentCategoryIcon)
            findNavController().navigate(action)
        }

        binding.ibEditCategoryName.setOnClickListener {
            val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditNameDialogFragment(args.categoryId, currentCategoryName)
            findNavController().navigate(action)
        }

        binding.ibEditCategoryNature.setOnClickListener {
            val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditNatureDialogFragment(args.categoryId, currentCategoryNature)
            findNavController().navigate(action)
        }

        binding.fabAddSubcategory.setOnClickListener {
            toolbar.title = getString(R.string.add_subcategory)
            val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToSubcategoryAddFragment(args.categoryId)
            findNavController().navigate(action)
        }
    }

    override fun onStop() {
        super.onStop()
        categoryReference.removeEventListener(selectedCategoryListener)
        subcategoryReference.removeEventListener(subcategoriesListener)
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    private fun setSelectedCategoryListener() {
        selectedCategoryListener = object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val category = snapshot.getValue<Category>()
                if (category != null) {
                    currentCategoryColor = category.category_color.toString()
                    currentCategoryIcon = category.category_icon.toString()
                    currentCategoryName = category.category_name.toString()
                    currentCategoryNature = when (category.category_nature) {
                        0 -> "Essentials"
                        1 -> "Wants"
                        2 -> "Savings"
                        else -> "?"
                    }

                    val iconColor = resources.getIdentifier(
                        category.category_color.toString(),
                        "color",
                        activity.packageName
                    )

                    binding.flCategoryIcon.backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

                    val icon = resources.getIdentifier(
                        category.category_icon.toString(),
                        "drawable",
                        activity.packageName
                    )

                    binding.ivCategoryIcon.setImageResource(icon)
                    binding.ivCategoryIcon.setColorFilter(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.white,
                            null
                        )
                    )

                    binding.tvSelectedCategoryName.text = category.category_name
                    when (category.category_nature) {
                        0 -> binding.tvSelectedCategoryNature.text = getString(R.string.essentials)
                        1 -> binding.tvSelectedCategoryNature.text = getString(R.string.wants)
                        2 -> binding.tvSelectedCategoryNature.text = getString(R.string.savings)
                        else -> binding.tvSelectedCategoryNature.text = "?"
                    }

                    hideProgressDialog()
                }

                if (subcategoryAdapter.itemCount >= 20) {
                    binding.fabAddSubcategory.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar
                    .make(rootLayout, error.message, 5000)
                    .show()
            }
        }
    }

    private fun setSubcategoriesListener() {
        subcategoryAdapter = SubcategoryAdapter(mutableListOf(), this)
        binding.rvSubcategories.adapter = subcategoryAdapter
        binding.rvSubcategories.layoutManager = LinearLayoutManager(activity)

        subcategoriesListener = object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val subcategory = snapshot.getValue<Subcategory>()
                if (subcategory != null) {
                    subcategoryAdapter.addSubcategory(subcategory)
                }

                if (subcategoryAdapter.itemCount <= 0) {
                    binding.tvSubcategoriesEmpty.visibility = View.VISIBLE
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // no implementation
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                if (subcategoryAdapter.itemCount <= 0) {
                    binding.tvSubcategoriesEmpty.visibility = View.VISIBLE
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // no implementation
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar
                    .make(rootLayout, error.message, 5000)
                    .show()
            }
        }
    }

    override fun showPopup(view: View, position: Int) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.optionEdit-> {
                    toolbar.title = getString(R.string.edit_subcategory)
                    val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToSubcategoryEditFragment(args.categoryId, view.tag.toString())
                    findNavController().navigate(action)
                    true
                }
                R.id.optionDelete-> {
                    confirmDelete(view.tag.toString(), position)
                    true
                }
                else -> false
            }
        }
        popup.menuInflater.inflate(R.menu.edit_options_2_menu, popup.menu)
        popup.show()
    }

    private fun confirmDelete(subcategoryId: String, position: Int) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.delete_subcategory_mark))
            .setMessage(resources.getString(R.string.delete_subcategory_confirm))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteSubcategory(subcategoryId, position) }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deleteSubcategory(subcategoryId: String, position: Int) {
        subcategoryReference.child(subcategoryId).removeValue()
            .addOnSuccessListener {
                subcategoryAdapter.removeSubcategory(position)
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
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
        binding.pbCategoryEdit.visibility = View.VISIBLE
        binding.llCategoryEdit.visibility = View.GONE
        binding.rvSubcategories.visibility = View.GONE
        binding.fabAddSubcategory.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbCategoryEdit.visibility = View.INVISIBLE
        binding.llCategoryEdit.visibility = View.VISIBLE
        binding.rvSubcategories.visibility = View.VISIBLE
        binding.fabAddSubcategory.visibility = View.VISIBLE
    }
}