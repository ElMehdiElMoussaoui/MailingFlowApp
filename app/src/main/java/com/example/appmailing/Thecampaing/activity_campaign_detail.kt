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
import androidx.core.content.ContextCompat
import com.example.appmailing.R
import com.example.appmailing.TheProducts.Product
import com.example.appmailing.statistique.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CampaignDetailActivity : AppCompatActivity() {

    private val viewModel: CampaignViewModel by viewModels()
    private var campaignId: Int = -1
    private var currentCampaign: Campaign? = null

    private lateinit var ivCampaignImage: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentCampaign?.let { campaign ->
                val updatedCampaign = campaign.copy(imageUri = it.toString())
                viewModel.updateCampaign(updatedCampaign)
                ivCampaignImage.setImageURI(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campaign_detail)

        campaignId = intent.getIntExtra("CAMPAIGN_ID", -1)
        
        val tvTitle       = findViewById<TextView>(R.id.tvCampaignTitle)
        val tvStatus      = findViewById<TextView>(R.id.tvStatusBadge)
        val tvRecipients  = findViewById<TextView>(R.id.tvRecipients)
        val tvTimestamp   = findViewById<TextView>(R.id.tvTimestamp)
        val tvEmailSubject = findViewById<TextView>(R.id.tvEmailSubject)
        val tvProductInfo = findViewById<TextView>(R.id.tvProductInfo)
        val ivBack        = findViewById<ImageView>(R.id.ivArrow)
        val btnEdit       = findViewById<MaterialButton>(R.id.btnEditCampaign)
        val btnDelete     = findViewById<ImageButton>(R.id.btnDeleteCampaign)
        val btnEditImage  = findViewById<View>(R.id.btnEditImage)
        ivCampaignImage   = findViewById(R.id.ivCampaignImage)

        ivBack.setOnClickListener { finish() }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        btnEditImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        if (campaignId != -1) {
            viewModel.allCampaignsFullDetails.observe(this) { allDetails ->
                val details = allDetails.find { it.campaign.id == campaignId }
                details?.let { d ->
                    currentCampaign = d.campaign
                    val campaign = d.campaign
                    val product = d.product

                    tvTitle.text = campaign.title
                    tvEmailSubject.text = campaign.emailSubject
                    tvRecipients.text = "${campaign.recipientCount} Recipients"
                    tvTimestamp.text = formatTimestamp(campaign.timestamp)
                    tvProductInfo.text = product?.name ?: "No Product Linked"
                    
                    if (campaign.imageUri.isNotEmpty()) {
                        try {
                            ivCampaignImage.setImageURI(Uri.parse(campaign.imageUri))
                        } catch (e: Exception) {
                            ivCampaignImage.setImageResource(R.drawable.ic_placeholder_img)
                        }
                    } else {
                        ivCampaignImage.setImageResource(R.drawable.ic_placeholder_img)
                    }
                    
                    applyStatusStyle(tvStatus, campaign.status)

                    btnEdit.setOnClickListener {
                        showEditDialog(d)
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Campaign")
            .setMessage("Are you sure you want to delete this campaign?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCampaign(campaignId)
                Toast.makeText(this, "Campaign deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(details: CampaignFullDetails) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_template, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etSubject = view.findViewById<EditText>(R.id.etSubject)
        val etSearchProduct = view.findViewById<EditText>(R.id.etSearchProduct)
        val spinnerProduct = view.findViewById<Spinner>(R.id.spinnerProduct)
        
        // Hide the image picker section in the edit dialog
        view.findViewById<View>(R.id.imagePickerSection)?.visibility = View.GONE

        etName.setText(details.campaign.title)
        etSubject.setText(details.campaign.emailSubject)

        CoroutineScope(Dispatchers.IO).launch {
            val allProducts = AppDatabase.getDatabase(this@CampaignDetailActivity).productDao().getAllProductsList()
            withContext(Dispatchers.Main) {
                var currentFilteredProducts = allProducts

                fun updateSpinner(products: List<Product>) {
                    val adapter = ArrayAdapter(this@CampaignDetailActivity, android.R.layout.simple_spinner_dropdown_item, products.map { it.name })
                    spinnerProduct.adapter = adapter
                    
                    // Restore selection if product exists in filtered list
                    if (products == allProducts) {
                        val currentProdPos = products.indexOfFirst { it.id == details.campaign.productId }
                        if (currentProdPos != -1) spinnerProduct.setSelection(currentProdPos)
                    }
                }

                updateSpinner(allProducts)

                etSearchProduct.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val query = s.toString().lowercase()
                        currentFilteredProducts = allProducts.filter { it.name.lowercase().contains(query) }
                        updateSpinner(currentFilteredProducts)
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                AlertDialog.Builder(this@CampaignDetailActivity)
                    .setTitle("Edit Campaign")
                    .setView(view)
                    .setPositiveButton("Update") { _, _ ->
                        val newTitle = etName.text.toString().trim()
                        val newSubject = etSubject.text.toString().trim()
                        
                        val selectedProduct = if (spinnerProduct.selectedItemPosition != -1 && currentFilteredProducts.isNotEmpty()) {
                            currentFilteredProducts[spinnerProduct.selectedItemPosition]
                        } else null

                        if (newTitle.isNotEmpty() && newSubject.isNotEmpty()) {
                            val updatedCampaign = details.campaign.copy(
                                title = newTitle,
                                emailSubject = newSubject,
                                productId = selectedProduct?.id ?: details.campaign.productId
                            )
                            viewModel.updateCampaign(updatedCampaign)
                            Toast.makeText(this@CampaignDetailActivity, "Campaign updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun formatTimestamp(timestampStr: String): String {
        return try {
            val millis = timestampStr.toLong()
            val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
            sdf.format(Date(millis))
        } catch (e: Exception) {
            timestampStr
        }
    }

    private fun applyStatusStyle(tv: TextView, status: String) {
        val ctx = tv.context
        tv.text = status.uppercase()
        when (status.uppercase()) {
            "SENT" -> {
                tv.setBackgroundResource(R.drawable.status_sent_bg)
                tv.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_green_dark))
            }
            "FAILED" -> {
                tv.setBackgroundResource(R.drawable.status_failed_bg)
                tv.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark))
            }
            else -> {
                tv.setBackgroundResource(R.drawable.status_pending_bg)
                tv.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_blue_dark))
            }
        }
    }
}