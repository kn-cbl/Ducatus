package com.ducatus

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
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


class CategoryEditFragment : Fragment(), SubcategoryInterface, DialogInterface.OnDismissListener {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryEditBinding
    private lateinit var subcategoryAdapter: SubcategoryAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rootLayout: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: MaterialToolbar
    private lateinit var currentCategoryColor: String
    private lateinit var currentCategoryIcon: String
    private lateinit var currentCategoryName: String
    private lateinit var currentCategoryNature: String
    private val args: CategoryEditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        toolbar = activity.findViewById(R.id.tbCategories)
        toolbar.title = getString(R.string.edit_category)
        toolbar.inflateMenu(R.menu.check_menu)

        binding = FragmentCategoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.ibEditCategoryIcon.setOnClickListener {
            val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditIconFragment(args.categoryId, currentCategoryColor, currentCategoryIcon)
            findNavController().navigate(action)
        }

        binding.ibEditCategoryName.setOnClickListener {
//            val fragmentManager = fragmentManager
//            val editNameFragment = CategoryEditNameFragment()
//            if (fragmentManager != null) {
//                editNameFragment.show(fragmentManager, "dialog")
//            }
            val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditNameFragment(args.categoryId, currentCategoryName)
            findNavController().navigate(action)
        }

        binding.ibEditCategoryNature.setOnClickListener {
            val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditNatureFragment(args.categoryId, currentCategoryNature)
            findNavController().navigate(action)
        }

    }

    override fun onDismiss(dialog: DialogInterface) {
        loadData()
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun showPopup(view: View) {
        val popup = PopupMenu(activity, view)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.optionEdit-> {
                    val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToSubcategoryEditFragment(view.tag.toString())
                    findNavController().navigate(action)
                    true
                }
                R.id.optionDelete-> {
                    confirmDelete(view.tag.toString())
                    true
                }
                else -> false
            }
        }
        popup.menuInflater.inflate(R.menu.edit_options_2_menu, popup.menu)
        popup.show()
    }

    private fun confirmDelete(categoryId: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.delete_subcategory_mark))
            .setMessage(resources.getString(R.string.delete_subcategory_confirm))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteSubcategory(categoryId) }
            .setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            .show()
    }

    private fun deleteSubcategory(subcategoryId: String) {
        databaseReference = databaseReference.child(subcategoryId)
        databaseReference.removeValue()
            .addOnSuccessListener {
                Snackbar
                    .make(rootLayout, "Successfully deleted subcategory", Snackbar.LENGTH_LONG)
                    .show()

                loadData()
                hideProgressDialog()
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, "Failed to delete subcategory", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { deleteSubcategory(subcategoryId) }
                    .show()
            }
    }

    private fun loadData() {
        showProgressDialog()
        auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            loadSelectedCategory(firebaseUser.uid, currentAccountId)
            loadSubcategories(firebaseUser.uid, currentAccountId)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadSelectedCategory(uid: String, currentAccountId: String) {
        databaseReference = database.getReference("categories").child(uid).child(currentAccountId).child(args.categoryId)
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
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
                    binding.tvSelectedCategoryName.text = category.category_name
                    when (category.category_nature) {
                        0 -> binding.tvSelectedCategoryNature.text = getString(R.string.essentials)
                        1 -> binding.tvSelectedCategoryNature.text = getString(R.string.wants)
                        2 -> binding.tvSelectedCategoryNature.text = getString(R.string.savings)
                        else -> binding.tvSelectedCategoryNature.text = "?"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar
                    .make(rootLayout, "Unable to load data, ${error.message}", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { loadSelectedCategory(uid, currentAccountId) }
                    .show()
            }
        })
    }

    private fun loadSubcategories(uid: String, currentAccountId: String) {
        subcategoryAdapter = SubcategoryAdapter(mutableListOf(), this)
        binding.rvSubcategories.adapter = subcategoryAdapter
        binding.rvSubcategories.layoutManager = LinearLayoutManager(activity)

        databaseReference = database.getReference("subcategories").child(uid).child(currentAccountId).child(args.categoryId)
        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val subcategory = child.getValue<Subcategory>()
                    if (subcategory != null) {
                        subcategoryAdapter.addSubcategory(subcategory)
                    }
                }

                hideProgressDialog()
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar
                    .make(rootLayout, "Failed to load data", Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) { loadSubcategories(uid, currentAccountId) }
                    .show()
            }
        })
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
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity.finish()
            }
        }.start()
    }

    private fun showProgressDialog() {
        binding.pbCategoryEdit.visibility = View.VISIBLE
        binding.llCategoryEdit.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbCategoryEdit.visibility = View.INVISIBLE
        binding.llCategoryEdit.visibility = View.VISIBLE
    }
}