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
                Toast.makeText(this, getString(R.string.please_select_campaign), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@HomeScreen, getString(R.string.no_contacts_found), Toast.LENGTH_LONG).show()
            } else {
                database.contactDao().resetAllSendingStatus()
                startMailingProcess(campaign)
            }
        }
    }

    private fun showCampaignSelectionDialog(tvStatus: TextView, ivPreview: ImageView) {
        if (campaignList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_campaigns_found), Toast.LENGTH_LONG).show()
            return
        }

        val names = campaignList.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_campaign))
            .setItems(names) { _, which ->
                val selected = campaignList[which]
                selectedCampaign = selected
                updateUI(selected, tvStatus, ivPreview)

                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("SELECTED_CAMPAIGN_ID", selected.id)
                    .apply()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun startMailingProcess(campaign: Campaign) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

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
        
        Toast.makeText(this, getString(R.string.mailing_started), Toast.LENGTH_SHORT).show()

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(mailWorkRequest.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val successCount = workInfo.outputData.getInt("success", 0)
                        val failedCount = workInfo.outputData.getInt("failed", 0)
                        
                        if (failedCount > 0) {
                            Toast.makeText(this, "Process finished. Sent: $successCount, Failed: $failedCount. Check Contact status!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "All emails sent successfully!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.mailing_failed), Toast.LENGTH_LONG).show()
                    }
                }
            })
    }
}
