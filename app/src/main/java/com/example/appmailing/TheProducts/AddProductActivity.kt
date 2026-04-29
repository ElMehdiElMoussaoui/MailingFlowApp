package com.example.appmailing.TheProducts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.appmailing.R
import java.util.Locale

class AddProductActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT        = "extra_product"
        const val EXTRA_RESULT_PRODUCT = "result_product"
    }

    private var editingProduct: Product? = null
    private var selectedImageUri: Uri?   = null

    private lateinit var layoutName: TextInputLayout
    private lateinit var layoutPrice: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var actvCurrency: AutoCompleteTextView
    private lateinit var ivProductImage: ImageView
    private lateinit var btnUploadImage: android.widget.LinearLayout
    private lateinit var btnSave: MaterialButton
    private lateinit var tvImageHint: TextView

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    val contentResolver = applicationContext.contentResolver
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                selectedImageUri = it
                ivProductImage.setImageURI(it)
                ivProductImage.visibility = android.view.View.VISIBLE
                tvImageHint.text = "Image sélectionnée ✓"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)
        bindViews()
        setupToolbar()
        setupCategoryDropdown()
        setupCurrencyDropdown()
        setupImageUpload()
        setupSaveButton()
        prefillIfEditing()
    }

    private fun bindViews() {
        layoutName    = findViewById(R.id.layoutName)
        layoutPrice   = findViewById(R.id.layoutPrice)
        etName        = findViewById(R.id.etName)
        etPrice       = findViewById(R.id.etPrice)
        etDescription = findViewById(R.id.etDescription)
        actvCategory  = findViewById(R.id.actvCategory)
        actvCurrency  = findViewById(R.id.actvCurrency)
        ivProductImage = findViewById(R.id.ivProductImage)
        btnUploadImage = findViewById(R.id.btnUploadImage)
        btnSave        = findViewById(R.id.btnSave)
        tvImageHint    = findViewById(R.id.tvImageHint)
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnSaveIcon).setOnClickListener { saveProduct() }
    }

    private fun setupCategoryDropdown() {
        val categories = ProductCategory.all.drop(1)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        actvCategory.setAdapter(adapter)
        actvCategory.threshold = 0
        actvCategory.setOnClickListener { actvCategory.showDropDown() }
    }

    private fun setupCurrencyDropdown() {
        val currencies = CurrencyType.all
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        actvCurrency.setAdapter(adapter)
        actvCurrency.threshold = 0
        
        // Set default currency if not editing
        if (editingProduct == null) {
            actvCurrency.setText(currencies[0], false)
        }
        
        actvCurrency.setOnClickListener { actvCurrency.showDropDown() }
    }

    private fun setupImageUpload() {
        btnUploadImage.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun prefillIfEditing() {
        editingProduct = intent.getSerializableExtra(EXTRA_PRODUCT) as? Product
        editingProduct?.let { p ->
            etName.setText(p.name)
            
            val priceText = if (p.price % 1.0 == 0.0) {
                p.price.toInt().toString()
            } else {
                String.format(Locale.US, "%.2f", p.price)
            }
            etPrice.setText(priceText)
            
            actvCurrency.setText(p.currency, false)
            etDescription.setText(p.description)
            actvCategory.setText(p.category, false)
            if (!p.imageUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(p.imageUri)
                    ivProductImage.setImageURI(uri)
                    ivProductImage.visibility = android.view.View.VISIBLE
                    selectedImageUri = uri
                    tvImageHint.text = "Image sélectionnée ✓"
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener { saveProduct() }
    }

    private fun saveProduct() {
        val name     = etName.text?.toString()?.trim() ?: ""
        var priceStr = etPrice.text?.toString()?.trim() ?: ""
        val currency = actvCurrency.text?.toString()?.trim() ?: "MAD"
        val desc     = etDescription.text?.toString()?.trim() ?: ""
        val category = actvCategory.text?.toString()?.trim() ?: ""

        priceStr = priceStr.replace(",", ".")

        var valid = true

        if (name.isEmpty()) {
            layoutName.error = "Le nom est requis"
            valid = false
        } else {
            layoutName.error = null
        }

        val price = priceStr.toDoubleOrNull()
        if (priceStr.isEmpty() || price == null || price < 0) {
            layoutPrice.error = "Prix invalide"
            valid = false
        } else {
            layoutPrice.error = null
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une catégorie", Toast.LENGTH_SHORT).show()
            valid = false
        }

        if (!valid) return

        val product = Product(
            id          = editingProduct?.id ?: 0,
            name        = name,
            category    = category,
            price       = price!!,
            currency    = currency,
            description = desc,
            imageUri    = selectedImageUri?.toString() ?: editingProduct?.imageUri
        )

        val result = Intent().apply {
            putExtra(EXTRA_RESULT_PRODUCT, product)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
