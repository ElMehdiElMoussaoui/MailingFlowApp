package com.example.appmailing.TheContact

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmailing.HomeScreen
import com.example.appmailing.R
import com.example.appmailing.Thecampaing.CampaignActivity
import com.example.appmailing.TheProducts.ProductsActivity
import com.example.appmailing.statistique.AppDatabase
import com.example.appmailing.statistique.StatistiqueActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory

class ContactsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var tvContactCount: TextView
    private lateinit var fabAddContact: FloatingActionButton
    private lateinit var btnImportContacts: MaterialButton
    private lateinit var tvSortByName: TextView

    private lateinit var toolbarNormal: View
    private lateinit var toolbarSearch: View
    private lateinit var toolbarSelection: View
    private lateinit var tvSelectionCount: TextView
    private lateinit var cbSelectAll: CheckBox
    private lateinit var btnDeleteSelected: ImageButton
    private lateinit var btnCancelSelection: ImageButton
    private lateinit var btnImportHeader: ImageButton

    private lateinit var etSearch: TextInputEditText
    private lateinit var btnSearch: ImageButton
    private lateinit var btnFilter: ImageButton
    private lateinit var btnSearchClose: ImageButton
    private lateinit var btnSearchClear: ImageButton

    private lateinit var panelFilters: View
    private lateinit var btnConforme: MaterialButton
    private lateinit var btnNonConforme: MaterialButton
    private lateinit var btnEnvoye: MaterialButton
    private lateinit var btnNonEnvoye: MaterialButton

    private lateinit var db: AppDatabase
    private var fullContactsList = listOf<Contact>()
    private var searchQuery: String = ""
    private var activeEmailFilter: EmailStatus? = null
    private var activeSendingFilter: SendingStatus? = null
    private var filtersVisible = false
    private var isSortRecent = false 

    private val contactFormLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Snackbar.make(recyclerView, getString(R.string.product_updated, ""), Snackbar.LENGTH_SHORT).show()
            }
        }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleFileImport(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        
        db = AppDatabase.getDatabase(this)

        bindViews()
        setupRecyclerView()
        setupToolbarSearch()
        setupSelectionToolbar()
        setupFilters()
        setupButtons()
        setupBottomNav()
        observeDatabase()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun bindViews() {
        recyclerView      = findViewById(R.id.recyclerViewContacts)
        tvContactCount    = findViewById(R.id.tvContactCount)
        fabAddContact     = findViewById(R.id.fabAddContact)
        btnImportContacts = findViewById(R.id.btnImportContacts)
        tvSortByName      = findViewById(R.id.tvSortByName)

        toolbarNormal    = findViewById(R.id.toolbarNormal)
        toolbarSearch    = findViewById(R.id.toolbarSearch)
        toolbarSelection = findViewById(R.id.toolbarSelection)
        tvSelectionCount = findViewById(R.id.tvSelectionCount)
        cbSelectAll      = findViewById(R.id.cbSelectAll)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)
        btnCancelSelection = findViewById(R.id.btnCancelSelection)
        btnImportHeader    = findViewById(R.id.btnImport)

        etSearch        = findViewById(R.id.etSearch)
        btnSearch       = findViewById(R.id.btnSearch)
        btnFilter       = findViewById(R.id.btnFilter)
        btnSearchClose  = findViewById(R.id.btnSearchClose)
        btnSearchClear  = findViewById(R.id.btnSearchClear)

        panelFilters    = findViewById(R.id.panelFilters)
        btnConforme     = findViewById(R.id.btnConforme)
        btnNonConforme  = findViewById(R.id.btnNonConforme)
        btnEnvoye       = findViewById(R.id.btnEnvoye)
        btnNonEnvoye    = findViewById(R.id.btnNonEnvoye)
        
        tvSortByName.text = getString(R.string.sort_by_recent)
    }

    private fun observeDatabase() {
        db.contactDao().getAllContacts().observe(this) { contacts ->
            fullContactsList = contacts
            applyFilters()
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(
            onContactClick = { contact -> openDetail(contact) },
            onDeleteClick = { contact -> deleteContact(contact) },
            onEditClick = { contact -> openEdit(contact) },
            onSelectionChanged = { count ->
                tvSelectionCount.text = "$count selected"
                cbSelectAll.isChecked = count > 0 && count == adapter.itemCount
                
                if (count > 0 && toolbarSelection.visibility == View.GONE) {
                    toolbarNormal.visibility = View.GONE
                    toolbarSelection.visibility = View.VISIBLE
                } else if (count == 0 && toolbarSelection.visibility == View.VISIBLE) {
                    toolbarSelection.visibility = View.GONE
                    toolbarNormal.visibility = View.VISIBLE
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSelectionToolbar() {
        btnCancelSelection.setOnClickListener {
            adapter.setSelectionMode(false)
            toolbarSelection.visibility = View.GONE
            toolbarNormal.visibility = View.VISIBLE
        }

        cbSelectAll.setOnClickListener {
            if (cbSelectAll.isChecked) adapter.selectAll() else adapter.clearSelection()
        }

        btnDeleteSelected.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            if (selectedIds.isEmpty()) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Delete Selected")
                .setMessage("Are you sure you want to delete ${selectedIds.size} contacts?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val contactsToDelete = fullContactsList.filter { it.id in selectedIds }
                        contactsToDelete.forEach { db.contactDao().deleteContact(it) }
                        withContext(Dispatchers.Main) {
                            adapter.setSelectionMode(false)
                            toolbarSelection.visibility = View.GONE
                            toolbarNormal.visibility = View.VISIBLE
                            Snackbar.make(recyclerView, "${selectedIds.size} contacts deleted", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteContact(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_delete))
            .setMessage(getString(R.string.delete_campaign_msg))
            .setPositiveButton(getString(R.string.menu_delete)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.contactDao().deleteContact(contact)
                    withContext(Dispatchers.Main) {
                        Snackbar.make(recyclerView, getString(R.string.contact_deleted, contact.fullName), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.action_cancel)) {
                                lifecycleScope.launch(Dispatchers.IO) { db.contactDao().insertContact(contact) }
                            }.show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun openEdit(contact: Contact) {
        val intent = Intent(this, AddContactActivity::class.java).apply {
            putExtra(AddContactActivity.EXTRA_CONTACT_ID,    contact.id)
            putExtra(AddContactActivity.EXTRA_CONTACT_NAME,  contact.fullName)
            putExtra(AddContactActivity.EXTRA_CONTACT_EMAIL, contact.email)
            putExtra(AddContactActivity.EXTRA_CONTACT_PHONE, contact.phone)
        }
        contactFormLauncher.launch(intent)
    }

    private fun openDetail(contact: Contact) {
        val intent = Intent(this, ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.id)
            putExtra(ContactDetailActivity.EXTRA_CONTACT_NAME, contact.fullName)
            putExtra(ContactDetailActivity.EXTRA_CONTACT_EMAIL, contact.email)
            putExtra(ContactDetailActivity.EXTRA_CONTACT_PHONE, contact.phone)
            putExtra(ContactDetailActivity.EXTRA_EMAIL_STATUS, contact.emailStatus)
            putExtra(ContactDetailActivity.EXTRA_SENDING_STATUS, contact.sendingStatus)
        }
        contactFormLauncher.launch(intent)
    }

    private fun setupToolbarSearch() {
        btnSearch.setOnClickListener {
            toolbarNormal.visibility = View.GONE
            toolbarSearch.visibility = View.VISIBLE
            etSearch.requestFocus()
        }

        btnSearchClose.setOnClickListener { closeSearch() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                btnSearchClear.visibility = if (searchQuery.isNotBlank()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSearchClear.setOnClickListener {
            etSearch.text?.clear()
        }
    }

    private fun applyFilters() {
        val baseList = if (isSortRecent) {
            fullContactsList.sortedByDescending { it.id }
        } else {
            fullContactsList.sortedBy { it.fullName }
        }
        adapter.filter(baseList, searchQuery, activeEmailFilter, activeSendingFilter)
        tvContactCount.text = getString(R.string.all_contacts, adapter.itemCount)
        
        val showInfo = adapter.itemCount == 0 && searchQuery.isEmpty() && activeEmailFilter == null && activeSendingFilter == null
        findViewById<View>(R.id.layoutInfo).visibility = if (showInfo) View.VISIBLE else View.GONE
    }

    private fun setupFilters() {
        btnFilter.setOnClickListener {
            filtersVisible = !filtersVisible
            panelFilters.visibility = if (filtersVisible) View.VISIBLE else View.GONE
            btnFilter.setColorFilter(if (filtersVisible) Color.parseColor("#2979FF") else Color.parseColor("#555555"))
        }

        btnConforme.setOnClickListener { toggleEmailFilter(EmailStatus.CONFORME) }
        btnNonConforme.setOnClickListener { toggleEmailFilter(EmailStatus.NON_CONFORME) }
        btnEnvoye.setOnClickListener { toggleSendingFilter(SendingStatus.SENT) }
        btnNonEnvoye.setOnClickListener { toggleSendingFilter(SendingStatus.PENDING) }
    }

    private fun toggleEmailFilter(status: EmailStatus) {
        activeEmailFilter = if (activeEmailFilter == status) null else status
        updateFilterButtonStates()
        applyFilters()
    }

    private fun toggleSendingFilter(status: SendingStatus) {
        activeSendingFilter = if (activeSendingFilter == status) null else status
        updateFilterButtonStates()
        applyFilters()
    }

    private fun updateFilterButtonStates() {
        val activeColor = Color.parseColor("#2979FF")
        val inactiveColor = Color.parseColor("#E8EAF6")
        val activeTextColor = Color.parseColor("#FFFFFF")
        val inactiveTextColor = Color.parseColor("#555555")

        fun updateBtn(btn: MaterialButton, isActive: Boolean) {
            btn.backgroundTintList = ColorStateList.valueOf(if (isActive) activeColor else inactiveColor)
            btn.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
        }

        updateBtn(btnConforme, activeEmailFilter == EmailStatus.CONFORME)
        updateBtn(btnNonConforme, activeEmailFilter == EmailStatus.NON_CONFORME)
        updateBtn(btnEnvoye, activeSendingFilter == SendingStatus.SENT)
        updateBtn(btnNonEnvoye, activeSendingFilter == SendingStatus.PENDING)
    }

    private fun handleFileImport(uri: Uri) {
        val fileName = getFileName(uri)
        if (fileName != null && (fileName.endsWith(".csv", true) || fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true))) {
            if (fileName.endsWith(".csv", true)) importFromCsv(uri) else importFromExcel(uri)
        } else {
            Snackbar.make(recyclerView, "Format non supporté", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun importFromCsv(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            var imported = 0
            try {
                contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.forEachIndexed { idx, line ->
                        if (idx == 0 || line.isBlank()) return@forEachIndexed
                        val cols = line.split(",").map { it.trim() }
                        if (cols.size >= 2) {
                            val contact = Contact(fullName = cols[0], email = cols[1], phone = if(cols.size > 2) cols[2] else "")
                            db.contactDao().insertContact(contact)
                            imported++
                        }
                    }
                }
                showImportResult(imported, "CSV")
            } catch (e: Exception) { showImportError() }
        }
    }

    private fun importFromExcel(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            var imported = 0
            val formatter = DataFormatter()
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    val sheet = workbook.getSheetAt(0)
                    for (row in sheet) {
                        if (row.rowNum == 0) continue
                        
                        val name = formatter.formatCellValue(row.getCell(0)).trim()
                        val email = formatter.formatCellValue(row.getCell(1)).trim()
                        val phone = formatter.formatCellValue(row.getCell(2)).trim()
                        
                        if (name.isNotBlank() && email.isNotBlank()) {
                            db.contactDao().insertContact(Contact(fullName = name, email = email, phone = phone))
                            imported++
                        }
                    }
                    workbook.close()
                }
                showImportResult(imported, "Excel")
            } catch (e: Exception) { 
                Log.e("ImportExcel", "Error: ${e.message}", e)
                showImportError() 
            }
        }
    }

    private suspend fun showImportResult(count: Int, type: String) {
        withContext(Dispatchers.Main) {
            Snackbar.make(recyclerView, getString(R.string.import_success, count), Snackbar.LENGTH_SHORT).show()
        }
    }

    private suspend fun showImportError() {
        withContext(Dispatchers.Main) {
            Snackbar.make(recyclerView, getString(R.string.import_error), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    private fun setupButtons() {
        fabAddContact.setOnClickListener {
            contactFormLauncher.launch(Intent(this, AddContactActivity::class.java))
        }
        btnImportContacts.setOnClickListener {
            showImportFormatDialog()
        }
        btnImportHeader.setOnClickListener {
            showImportFormatDialog()
        }
        tvSortByName.setOnClickListener {
            isSortRecent = !isSortRecent
            tvSortByName.text = if (isSortRecent) getString(R.string.sort_by_name) else getString(R.string.sort_by_recent)
            applyFilters()
            val msg = if (isSortRecent) "Trier par Récent" else "Trier par Nom (A-Z)"
            Snackbar.make(recyclerView, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showImportFormatDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_import_contacts))
            .setMessage(getString(R.string.import_contacts_msg))
            .setPositiveButton("Sélectionner le fichier") { _, _ ->
                importFileLauncher.launch("*/*")
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun closeSearch() {
        toolbarSearch.visibility = View.GONE
        toolbarNormal.visibility = View.VISIBLE
        etSearch.setText("")
        searchQuery = ""
        applyFilters()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_contacts

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeScreen::class.java))
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_campaigns -> {
                    startActivity(Intent(this, CampaignActivity::class.java))
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_stats -> {
                    startActivity(Intent(this, StatistiqueActivity::class.java))
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_contacts -> true

                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (toolbarSelection.visibility == View.VISIBLE) {
            adapter.setSelectionMode(false)
            toolbarSelection.visibility = View.GONE
            toolbarNormal.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }
}
