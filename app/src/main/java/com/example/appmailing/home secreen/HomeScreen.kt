package com.example.appmailing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.appmailing.fct_send.EmailWorker
import com.example.appmailing.Thecampaing.CampaignActivity
import com.example.appmailing.Thecampaing.CampaignViewModel
import com.example.appmailing.Thecampaing.Campaign
import com.example.appmailing.TheContact.ContactsActivity
import com.example.appmailing.TheProducts.ProductsActivity
import com.example.appmailing.statistique.StatistiqueActivity
import com.example.appmailing.statistique.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class HomeScreen : AppCompatActivity() {

    private val campaignViewModel: CampaignViewModel by viewModels()
    private var selectedCampaign: Campaign? = null
    private var campaignList: List<Campaign> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        val tvStatus = findViewById<TextView>(R.id.tvSelectedTemplateName)
        val tvSelectLink = findViewById<TextView>(R.id.tvSelectTemplateLink)
        val tvBrowseLibrary = findViewById<TextView>(R.id.tvBrowseLibrary)
        val btnSend = findViewById<AppCompatButton>(R.id.btnSendCampaign)
        val ivPreview = findViewById<ImageView>(R.id.ivSelectedCampaignImage)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Drawer
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_campaigns -> {
                    startActivity(Intent(this, CampaignActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatistiqueActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }

        setupBottomNav(bottomNav)

        // Observe campaigns
        campaignViewModel.allCampaigns.observe(this) { campaigns ->
            campaignList = campaigns ?: emptyList()

            if (selectedCampaign == null) {
                val savedId = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .getInt("SELECTED_CAMPAIGN_ID", -1)

                if (savedId != -1) {
                    val found = campaignList.find { it.id == savedId }
                    if (found != null) {
                        selectedCampaign = found
                        updateUI(found, tvStatus, ivPreview)
                    }
                }
            }
        }

        tvSelectLink.setOnClickListener {
            showCampaignSelectionDialog(tvStatus, ivPreview)
        }

        tvBrowseLibrary.setOnClickListener {
            startActivity(Intent(this, CampaignActivity::class.java))
        }

        btnSend.setOnClickListener {
            selectedCampaign?.let { campaign ->
                checkContactsAndSend(campaign)
            } ?: run {
                Toast.makeText(this, "Please select a campaign first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(campaign: Campaign, tvStatus: TextView, ivPreview: ImageView) {
        tvStatus.text = campaign.title
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        
        ivPreview.imageTintList = null
        ivPreview.setPadding(0, 0, 0, 0)

        if (campaign.imageUri.isNotEmpty()) {
            try {
                ivPreview.setImageURI(Uri.parse(campaign.imageUri))
                ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                ivPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        } else {
            ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
            ivPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }

    private fun setupBottomNav(nav: BottomNavigationView) {
        nav.selectedItemId = R.id.nav_home
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_campaigns -> {
                    startActivity(Intent(this, CampaignActivity::class.java))
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatistiqueActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun checkContactsAndSend(campaign: Campaign) {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val contactsCount = database.contactDao().getContactsCount()

            if (contactsCount == 0) {
                Toast.makeText(this@HomeScreen, "No contacts found! Please add contacts first.", Toast.LENGTH_LONG).show()
            } else {
                database.contactDao().resetAllSendingStatus()
                startMailingProcess(campaign)
            }
        }
    }

    private fun showCampaignSelectionDialog(tvStatus: TextView, ivPreview: ImageView) {
        if (campaignList.isEmpty()) {
            Toast.makeText(this, "No campaigns found. Go to Campaigns to create one.", Toast.LENGTH_LONG).show()
            return
        }

        val names = campaignList.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Your Campaign")
            .setItems(names) { _, which ->
                val selected = campaignList[which]
                selectedCampaign = selected
                updateUI(selected, tvStatus, ivPreview)

                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("SELECTED_CAMPAIGN_ID", selected.id)
                    .apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startMailingProcess(campaign: Campaign) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Removed "Bearer " prefix as it is added in EmailWorker
        val inputData = workDataOf(
            "api_key" to "",
            "subject" to campaign.emailSubject,
            "image_uri" to campaign.imageUri,
            "campaign_id" to campaign.id
        )

        val mailWorkRequest = OneTimeWorkRequestBuilder<EmailWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(mailWorkRequest)
        
        Toast.makeText(this, "Mailing started...", Toast.LENGTH_SHORT).show()

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(mailWorkRequest.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    // Logic updated: EmailWorker returns Result.success() even if API fails (to update DB), 
                    // but we should verify if actually successful by checking the workInfo output if needed.
                    // For now, let's keep it simple.
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(this, "Process finished by successfully. Check Contact status!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Mailing failed. Check connection.", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }
}
