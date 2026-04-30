package com.example.appmailing.TheProducts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmailing.HomeScreen
import com.example.appmailing.R
import com.example.appmailing.TheContact.ContactsActivity
import com.example.appmailing.Thecampaing.CampaignActivity
import com.example.appmailing.statistique.StatistiqueActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ProductsActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductsAdapter
    private lateinit var tvItemCount: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var toolbarNormal: View
    private lateinit var btnSearch: ImageButton
    private lateinit var btnFilter: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var toolbarSearch: View
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnSearchClose: ImageButton
    private lateinit var btnSearchClear: ImageButton
    private lateinit var panelFilters: View
    private lateinit var btnAll: MaterialButton
    private lateinit var btnElectronics: MaterialButton
    private lateinit var btnWearables: MaterialButton
    private lateinit var btnFitness: MaterialButton
    private lateinit var btnAudio: MaterialButton
    private lateinit var btnOffice: MaterialButton

    private var searchQuery    = ""
    private var activeCategory = ProductCategory.ALL
    private var fullProductsList = listOf<Product>()
    private var filtersVisible = false

    private val addLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val p = result.data?.getSerializableExtra(AddProductActivity.EXTRA_RESULT_PRODUCT) as? Product
                p?.let {
                    viewModel.insert(it)
                    Snackbar.make(recyclerView, "\"${it.name}\" added!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                when {
                    data.getBooleanExtra(ProductDetailActivity.EXTRA_DELETED, false) -> {
                        val product = data.getSerializableExtra(ProductDetailActivity.EXTRA_PRODUCT) as? Product
                        product?.let { p ->
                            viewModel.delete(p)
                            Snackbar.make(recyclerView, "\"${p.name}\" deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo") { viewModel.insert(p) }.show()
                        }
                    }
                    data.getBooleanExtra(ProductDetailActivity.EXTRA_EDIT_REQUESTED, false) -> {
                        val product = data.getSerializableExtra(ProductDetailActivity.EXTRA_PRODUCT) as? Product
                        product?.let {
                            val intent = Intent(this, AddProductActivity::class.java).apply {
                                putExtra(AddProductActivity.EXTRA_PRODUCT, it)
                            }
                            editLauncher.launch(intent)
                        }
                    }
                }
            }
        }

    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val p = result.data?.getSerializableExtra(AddProductActivity.EXTRA_RESULT_PRODUCT) as? Product
                p?.let {
                    viewModel.update(it)
                    Snackbar.make(recyclerView, "\"${it.name}\" updated!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)
        bindViews()
        setupRecyclerView()
        setupToolbarSearch()
        setupFilterPanel()
        setupCategoryChips()
        setupFab()
        setupBottomNav()

        viewModel.allProducts.observe(this) { products ->
            fullProductsList = products
            applyFilter()
        }
    }

    private fun bindViews() {
        recyclerView    = findViewById(R.id.recyclerViewProducts)
        tvItemCount     = findViewById(R.id.tvItemCount)
        fabAdd          = findViewById(R.id.fabAdd)
        chipGroup       = findViewById(R.id.chipGroupCategories)
        toolbarNormal   = findViewById(R.id.toolbarNormal)
        btnBack         = findViewById(R.id.btnBack)
        btnSearch       = findViewById(R.id.btnSearch)
        btnFilter       = findViewById(R.id.btnFilter)
        toolbarSearch   = findViewById(R.id.toolbarSearch)
        etSearch        = findViewById(R.id.etSearch)
        btnSearchClose  = findViewById(R.id.btnSearchClose)
        btnSearchClear  = findViewById(R.id.btnSearchClear)
        panelFilters    = findViewById(R.id.panelFilters)
        btnAll          = findViewById(R.id.btnAll)
        btnElectronics  = findViewById(R.id.btnElectronics)
        btnWearables    = findViewById(R.id.btnWearables)
        btnFitness      = findViewById(R.id.btnFitness)
        btnAudio        = findViewById(R.id.btnAudio)
        btnOffice       = findViewById(R.id.btnOffice)
    }

    private fun setupRecyclerView() {
        adapter = ProductsAdapter { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra(ProductDetailActivity.EXTRA_PRODUCT, product)
            }
            detailLauncher.launch(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupToolbarSearch() {
        btnSearch.setOnClickListener {
            toolbarNormal.visibility = View.GONE
            toolbarSearch.visibility = View.VISIBLE
            etSearch.requestFocus()
        }
        btnSearchClose.setOnClickListener { closeSearch() }
        btnSearchClear.setOnClickListener { etSearch.setText("") }
        btnBack.setOnClickListener { finish() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                btnSearchClear.visibility = if (searchQuery.isNotBlank()) View.VISIBLE else View.GONE
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun setupCategoryChips() {
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(checkedIds[0])
            activeCategory = chip?.text?.toString() ?: ProductCategory.ALL
            applyFilter()
        }
    }

    private fun setupFilterPanel() {
        btnFilter.setOnClickListener {
            filtersVisible = !filtersVisible
            panelFilters.visibility = if (filtersVisible) View.VISIBLE else View.GONE
            btnFilter.setColorFilter(if (filtersVisible) getColor(R.color.blue_primary) else getColor(R.color.grey_dark))
        }

        val filterButtons = mapOf(
            btnAll to ProductCategory.ALL, btnElectronics to ProductCategory.ELECTRONICS,
            btnWearables to ProductCategory.WEARABLES, btnFitness to ProductCategory.FITNESS,
            btnAudio to ProductCategory.AUDIO, btnOffice to ProductCategory.OFFICE
        )

        filterButtons.forEach { (btn, cat) ->
            btn.setOnClickListener {
                activeCategory = cat
                updateFilterButtonStates(filterButtons)
                syncChipGroup(cat)
                applyFilter()
            }
        }
    }

    private fun syncChipGroup(category: String) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.text.toString() == category) {
                chip.isChecked = true
                break
            }
        }
    }

    private fun updateFilterButtonStates(buttons: Map<MaterialButton, String>) {
        buttons.forEach { (btn, cat) ->
            val isActive = cat == activeCategory
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isActive) getColor(R.color.blue_primary) else getColor(R.color.grey_light))
            btn.setTextColor(if (isActive) getColor(R.color.bg_white) else getColor(R.color.grey_dark))
        }
    }

    private fun applyFilter() {
        val filteredList = fullProductsList.filter { p ->
            val matchCat = activeCategory == ProductCategory.ALL || p.category == activeCategory
            val matchQuery = searchQuery.isBlank() || p.name.contains(searchQuery, true)
            matchCat && matchQuery
        }
        adapter.submitList(filteredList)
        tvItemCount.text = "AVAILABLE ITEMS (${filteredList.size})"
    }

    private fun setupFab() {
        fabAdd.setOnClickListener {
            addLauncher.launch(Intent(this, AddProductActivity::class.java))
        }
    }

    private fun closeSearch() {
        toolbarSearch.visibility = View.GONE
        toolbarNormal.visibility = View.VISIBLE
        etSearch.setText("")
        searchQuery = ""
        applyFilter()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_products

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeScreen::class.java))
                    true
                }

                R.id.nav_campaigns -> {
                    startActivity(Intent(this, CampaignActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_stats -> {
                    startActivity(Intent(this, StatistiqueActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_products -> true

                else -> false
            }
        }
    }
}
