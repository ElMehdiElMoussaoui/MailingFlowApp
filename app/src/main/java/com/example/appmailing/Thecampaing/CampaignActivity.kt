package com.example.appmailing.Thecampaing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmailing.HomeScreen
import com.example.appmailing.R
import com.example.appmailing.TheContact.ContactsActivity
import com.example.appmailing.TheProducts.Product
import com.example.appmailing.TheProducts.ProductsActivity
import com.example.appmailing.statistique.AppDatabase
import com.example.appmailing.statistique.StatistiqueActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CampaignActivity : AppCompatActivity() {

    private val viewModel: CampaignViewModel by viewModels()
    private lateinit var adapter: CampaignAdapter

    private var tempImageUri: Uri? = null
    private var ivDialogPreview: ImageView? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                try {
                    val contentResolver = applicationContext.contentResolver
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                tempImageUri = it
                ivDialogPreview?.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compaign_screen)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        setupHeaderActions()
        setupCreateButton()
        setupBottomNav()
        setupSort()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CampaignAdapter(
            fullList     = mutableListOf(),
            onItemClick  = { campaign -> openDetail(campaign) },
            onDeleteClick = { campaignId -> confirmDelete(campaignId) }
        )
        val rv = findViewById<RecyclerView>(R.id.rvCampaigns)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun observeViewModel() {
        val tvTotal = findViewById<TextView>(R.id.tvTotalCampaigns)
        viewModel.allCampaignsFullDetails.observe(this) { campaigns ->
            campaigns?.let {
                adapter.updateList(it)
                tvTotal.text = it.size.toString()
            }
        }
    }

    private fun setupHeaderActions() {
        val ivSearchIcon = findViewById<ImageView>(R.id.ivSearchIcon)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val tvTitleHeader = findViewById<TextView>(R.id.tvTitleHeader)
        val ivMenu = findViewById<ImageView>(R.id.ivMenuBtn)

        ivSearchIcon.setOnClickListener {
            if (etSearch.visibility == View.GONE) {
                etSearch.visibility = View.VISIBLE
                tvTitleHeader.visibility = View.GONE
                etSearch.requestFocus()
            } else {
                etSearch.visibility = View.GONE
                tvTitleHeader.visibility = View.VISIBLE
                etSearch.text.clear()
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ivMenu.setOnClickListener { view: View ->
            val popup = PopupMenu(this, view)
            popup.menu.add(getString(R.string.btn_pending))
            popup.menu.add(getString(R.string.btn_sent))
            popup.menu.add(getString(R.string.btn_failed))
            
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    getString(R.string.btn_pending) -> { adapter.sortByStatus("PENDING"); true }
                    getString(R.string.btn_sent) -> { adapter.sortByStatus("SENT"); true }
                    getString(R.string.btn_failed) -> { adapter.sortByStatus("FAILED"); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupSort() {
        val tvSort = findViewById<TextView>(R.id.tvSortByDate)
        tvSort.setOnClickListener {
            val currentList = viewModel.allCampaignsFullDetails.value ?: emptyList()
            adapter.updateList(currentList.sortedByDescending { it.campaign.timestamp })
            Toast.makeText(this, getString(R.string.sort_by_date), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCreateButton() {
        val btnCreate = findViewById<Button>(R.id.btnCreateTemplate)
        btnCreate.setOnClickListener { 
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@CampaignActivity)
                val products = withContext(Dispatchers.IO) { db.productDao().getAllProductsList() }
                
                if (products.isEmpty()) {
                    Toast.makeText(this@CampaignActivity, getString(R.string.create_product_first), Toast.LENGTH_LONG).show()
                } else {
                    showCreateDialog(products)
                }
            }
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_campaigns
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                     true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatistiqueActivity::class.java))
                     true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                     true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                     true
                }
                else -> false
            }
        }
    }

    private fun openDetail(campaign: Campaign) {
        val intent = Intent(this, CampaignDetailActivity::class.java)
        intent.putExtra("CAMPAIGN_ID", campaign.id)
        startActivity(intent)
    }

    private fun confirmDelete(campaignId: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_campaign))
            .setMessage(getString(R.string.delete_campaign_msg))
            .setPositiveButton(getString(R.string.btn_yes)) { _, _ ->
                viewModel.deleteCampaign(campaignId)
                Toast.makeText(this, getString(R.string.product_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_no), null)
            .show()
    }

    /**
     * DIALOG EXPLANATION:
     * - A Campaign is created by selecting a Product.
     * - Step 1: Select a Product.
     */
    private fun showCreateDialog(products: List<Product>) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_template, null)

        val etName      = view.findViewById<EditText>(R.id.etName)
        val etSubject   = view.findViewById<EditText>(R.id.etSubject)
        val etSearchProduct = view.findViewById<EditText>(R.id.etSearchProduct)
        val btnPick     = view.findViewById<Button>(R.id.btnPickImage)
        
        // Find spinners using exact IDs from XML
        val spinnerProduct = view.findViewById<Spinner>(R.id.spinnerProduct)
        
        ivDialogPreview = view.findViewById(R.id.ivSelectedImage)

        // Initial setup for the spinner
        var filteredProducts = products
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, products.map { it.name }.toMutableList())
        spinnerProduct.adapter = spinnerAdapter

        // Search logic
        etSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredProducts = products.filter { it.name.lowercase().contains(query) }
                spinnerAdapter.clear()
                spinnerAdapter.addAll(filteredProducts.map { it.name })
                spinnerAdapter.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnPick.setOnClickListener { pickImageLauncher.launch("image/*") }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_campaign))
            .setView(view)
            .setPositiveButton(getString(R.string.cd_btn_save)) { _, _ ->
                val name    = etName.text.toString().trim()
                val subject = etSubject.text.toString().trim()
                
                if (name.isEmpty() || subject.isEmpty()) {
                    Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedPosition = spinnerProduct.selectedItemPosition
                if (selectedPosition == -1 || filteredProducts.isEmpty()) {
                    Toast.makeText(this, getString(R.string.select_valid_product), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val selectedProduct = filteredProducts[selectedPosition]

                viewModel.addCampaign(
                    Campaign(
                        title          = name,
                        emailSubject   = subject,
                        status         = "PENDING",
                        timestamp      = System.currentTimeMillis().toString(),
                        imageUri       = tempImageUri?.toString() ?: "",
                        recipientCount = 0,
                        productId      = selectedProduct.id
                    )
                )
                tempImageUri    = null
                ivDialogPreview = null
                Toast.makeText(this, getString(R.string.campaign_created), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.action_cancel)) { _, _ ->
                tempImageUri    = null
                ivDialogPreview = null
            }
            .show()
    }
}
