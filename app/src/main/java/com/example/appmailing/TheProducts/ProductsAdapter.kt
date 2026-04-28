package com.example.appmailing.TheProducts

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmailing.R

class ProductsAdapter(
    private val onClick: (Product) -> Unit
) : ListAdapter<Product, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView   = itemView.findViewById(R.id.tvProductName)
        val tvCategory: TextView      = itemView.findViewById(R.id.tvCategory)
        val tvDescription: TextView   = itemView.findViewById(R.id.tvDescription)
        val tvPrice: TextView         = itemView.findViewById(R.id.tvPrice)

        fun bind(product: Product) {
            tvProductName.text = product.name
            tvCategory.text    = product.category.uppercase()
            tvDescription.text = product.description
            tvPrice.text       = "$${String.format("%.2f", product.price)}"

            // Image : URI utilisateur ou placeholder initiales
            if (!product.imageUri.isNullOrEmpty()) {
                try {
                    ivProductImage.setImageURI(Uri.parse(product.imageUri))
                } catch (e: Exception) {
                    setPlaceholder(product)
                }
            } else {
                setPlaceholder(product)
            }

            itemView.setOnClickListener { onClick(product) }
        }

        private fun setPlaceholder(product: Product) {
            ivProductImage.setImageResource(getCategoryIcon(product.category))
        }
    }

    private fun getCategoryIcon(category: String): Int = when (category) {
        ProductCategory.ELECTRONICS -> R.drawable.ic_category_electronics
        ProductCategory.WEARABLES   -> R.drawable.ic_category_wearables
        ProductCategory.FITNESS     -> R.drawable.ic_category_fitness
        ProductCategory.AUDIO       -> R.drawable.ic_category_audio
        ProductCategory.OFFICE      -> R.drawable.ic_category_office
        else                        -> R.drawable.ic_category_default
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(o: Product, n: Product)    = o.id == n.id
        override fun areContentsTheSame(o: Product, n: Product) = o == n
    }
}
