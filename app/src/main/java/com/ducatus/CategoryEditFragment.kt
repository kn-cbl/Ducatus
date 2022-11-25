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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ducatus.adapter.SubcategoryAdapter
import com.ducatus.data.Category
import com.ducatus.data.Subcategory
import com.ducatus.databinding.FragmentCategoryEditBinding
import com.ducatus.interfaces.SubcategoryInterface
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
    private lateinit var actionDialog: ActionDialogFragment
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentCategoryEditBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var categoryReference: DatabaseReference
    private lateinit var subcategoryReference: DatabaseReference
    private lateinit var subcategoriesListener: ChildEventListener
    private lateinit var subcategoryAdapter: SubcategoryAdapter
    private lateinit var currentCategoryColor: String
    private lateinit var currentCategoryIcon: String
    private lateinit var currentCategoryName: String
    private lateinit var currentCategoryNature: String
    private var firebaseUser: FirebaseUser? = null
    private val args: CategoryEditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.llCategories)
        toolbar = activity.findViewById(R.id.tbCategories)
        toolbar.title = getString(R.string.edit_category)
        toolbar.menu.clear()

        binding = FragmentCategoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        loadData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibEditCategoryIcon.setOnClickListener {
            firebaseUser?.let {
                val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditIconDialogFragment(args.categoryId, currentCategoryColor, currentCategoryIcon)
                findNavController().navigate(action)
            }
        }

        binding.ibEditCategoryName.setOnClickListener {
            firebaseUser?.let {
                val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditNameDialogFragment(args.categoryId, currentCategoryName)
                findNavController().navigate(action)
            }
        }

        binding.ibEditCategoryNature.setOnClickListener {
            firebaseUser?.let {
                val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToCategoryEditNatureDialogFragment(args.categoryId, currentCategoryNature)
                findNavController().navigate(action)
            }
        }

        binding.fabAddSubcategory.setOnClickListener {
            firebaseUser?.let {
                toolbar.title = getString(R.string.add_subcategory)
                val action = CategoryEditFragmentDirections.actionCategoryEditFragmentToSubcategoryAddFragment(args.categoryId)
                findNavController().navigate(action)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseUser?.let { subcategoryReference.removeEventListener(subcategoriesListener) }
    }

    // get activity to be used in adapter
    override fun getActivityInterface(): Activity {
        return activity
    }

    override fun onDismiss(p0: DialogInterface?) {
        loadCategory()
    }

    private fun loadData() {
        auth = Firebase.auth
        firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            sharedPreferences = SharedPreferences(activity)
            val currentAccountId = sharedPreferences.accountId.toString()

            database = Firebase.database
            categoryReference =
                database
                    .getReference("categories")
                    .child(firebaseUser!!.uid)
                    .child(currentAccountId)
                    .child(args.categoryId)

            setSubcategoriesListener()
            subcategoryReference =
                database
                    .getReference("subcategories")
                    .child(firebaseUser!!.uid)
                    .child(currentAccountId)
                    .child(args.categoryId)

            loadCategory()
            subcategoryReference.addChildEventListener(subcategoriesListener)
        }
        else {
            sessionExpired()
        }
    }

    private fun loadCategory() {
        showProgressDialog()
        categoryReference.get()
            .addOnSuccessListener { snapshot ->
                val category = snapshot.getValue<Category>()
                if (category != null) {
                    currentCategoryColor = category.color.toString()
                    currentCategoryIcon = category.icon.toString()
                    currentCategoryName = category.name.toString()
                    currentCategoryNature = when (category.nature) {
                        0 -> "Essentials"
                        1 -> "Wants"
                        2 -> "Savings"
                        else -> "?"
                    }

                    val iconColor = resources.getIdentifier(
                        category.color.toString(),
                        "color",
                        activity.packageName
                    )

                    binding.flCategoryIcon.backgroundTintList = ContextCompat.getColorStateList(activity, iconColor)

                    val icon = resources.getIdentifier(
                        category.icon.toString(),
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

                    binding.tvSelectedCategoryName.text = category.name
                    when (category.nature) {
                        0 -> binding.tvSelectedCategoryNature.text = getString(R.string.essentials)
                        1 -> binding.tvSelectedCategoryNature.text = getString(R.string.wants)
                        2 -> binding.tvSelectedCategoryNature.text = getString(R.string.savings)
                        else -> binding.tvSelectedCategoryNature.text = "?"
                    }

                    hideProgressDialog()
                }
            }
            .addOnFailureListener {
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
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

                val text = "${subcategoryAdapter.itemCount} / 30 subcategories"
                binding.tvSubcategoriesCount.text = text

                if (subcategoryAdapter.itemCount >= 30) {
                    binding.fabAddSubcategory.visibility = View.GONE
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // no implementation
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val text = "${subcategoryAdapter.itemCount} / 30 subcategories"
                binding.tvSubcategoriesCount.text = text

                if (subcategoryAdapter.itemCount >= 30) {
                    binding.fabAddSubcategory.visibility = View.GONE
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
        showProgressDialogDelete()
        subcategoryReference.child(subcategoryId).removeValue()
            .addOnSuccessListener {
                hideProgressDialogDelete()
                subcategoryAdapter.removeSubcategory(position)
            }
            .addOnFailureListener {
                hideProgressDialogDelete()
                Snackbar
                    .make(rootLayout, it.localizedMessage!!, 5000)
                    .show()
            }
    }

    private fun sessionExpired() {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.session_expired))
            .setPositiveButton(resources.getString(R.string.log_in)) { _, _ -> }

        dialog.setOnDismissListener {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity.finish()
        }

        dialog.show()
    }

    private fun showProgressDialog() {
        binding.pbCategoryEdit.visibility = View.VISIBLE
        binding.llCategoryEdit.visibility = View.GONE
        binding.tvSubcategoriesCount.visibility = View.GONE
        binding.rvSubcategories.visibility = View.GONE
        binding.fabAddSubcategory.visibility = View.GONE
    }

    private fun hideProgressDialog() {
        binding.pbCategoryEdit.visibility = View.INVISIBLE
        binding.llCategoryEdit.visibility = View.VISIBLE
        binding.tvSubcategoriesCount.visibility = View.VISIBLE
        binding.rvSubcategories.visibility = View.VISIBLE
        binding.fabAddSubcategory.visibility = View.VISIBLE
    }

    private fun showProgressDialogDelete() {
        val bundle = Bundle()
        bundle.putString("title", getString(R.string.deleting))

        actionDialog = ActionDialogFragment()
        actionDialog.arguments = bundle
        actionDialog.show(childFragmentManager, "dialog")
    }

    private fun hideProgressDialogDelete() {
        actionDialog.dismiss()
    }
}