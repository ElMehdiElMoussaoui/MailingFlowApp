package com.example.appmailing.TheProducts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.example.appmailing.R
import com.example.appmailing.statistique.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT       = "detail_product"
        const val EXTRA_DELETED       = "detail_deleted"
        const val EXTRA_EDIT_REQUESTED = "detail_edit_requested"
    }

    private lateinit var product: Product

    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedProduct = result.data?.getSerializableExtra(AddProductActivity.EXTRA_RESULT_PRODUCT) as? Product
            updatedProduct?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getDatabase(this@ProductDetailActivity).productDao().updateProduct(it)
                    withContext(Dispatchers.Main) {
                        product = it
                        bindProductData()
                        Toast.makeText(this@ProductDetailActivity, "Produit mis à jour", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        product = (intent.getSerializableExtra(EXTRA_PRODUCT) as? Product)
            ?: run { finish(); return }

        bindProductData()
        setupButtons()
    }

    private fun bindProductData() {
        val ivImage = findViewById<ImageView>(R.id.ivDetailImage)
        if (!product.imageUri.isNullOrEmpty()) {
            try {
                ivImage.setImageURI(Uri.parse(product.imageUri))
                ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                ivImage.setImageResource(getCategoryIcon(product.category))
                ivImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

        } else {
            ivImage.setImageResource(getCategoryIcon(product.category))
            ivImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        findViewById<TextView>(R.id.tvDetailName).text        = product.name
        findViewById<TextView>(R.id.tvDetailPrice).text       = "$${String.format("%.2f", product.price)}"
        findViewById<TextView>(R.id.tvDetailDescription).text = product.description
        findViewById<Chip>(R.id.chipDetailCategory).text      = product.category
    }

    private fun setupButtons() {
        findViewById<ImageButton>(R.id.btnDetailBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnDetailEdit).setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java).apply {
                putExtra(AddProductActivity.EXTRA_PRODUCT, product)
            }
            editLauncher.launch(intent)
        }

        findViewById<MaterialButton>(R.id.btnDetailDelete).setOnClickListener {
            val result = Intent().apply {
                putExtra(EXTRA_DELETED, true)
                putExtra(EXTRA_PRODUCT, product)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun getCategoryIcon(category: String): Int = when (category) {
        ProductCategory.ELECTRONICS -> R.drawable.ic_category_electronics
        ProductCategory.WEARABLES   -> R.drawable.ic_category_wearables
        ProductCategory.FITNESS     -> R.drawable.ic_category_fitness
        ProductCategory.AUDIO       -> R.drawable.ic_category_audio
        ProductCategory.OFFICE      -> R.drawable.ic_category_office
        else                        -> R.drawable.ic_placeholder_img
    }
}
