package com.example.mbaprototype.ui.products

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mbaprototype.R
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.ItemProductBinding // Import ViewBinding class

// Define lambda functions for click listeners
typealias OnProductClick = (Product) -> Unit
typealias OnAddToBasketClick = (Product) -> Unit

class ProductAdapter(
    private val onProductClick: OnProductClick,
    private val onAddToBasketClick: OnAddToBasketClick
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        // Inflate using ViewBinding
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding, onProductClick, onAddToBasketClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder class
    class ProductViewHolder(
        private val binding: ItemProductBinding, // Use ViewBinding
        private val onProductClick: OnProductClick,
        private val onAddToBasketClick: OnAddToBasketClick
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n") // Suppress warning for simple string concatenation
        fun bind(product: Product) {
            binding.textProductName.text = product.name
            binding.textProductPrice.text = String.format(
                itemView.context.getString(R.string.price_format), // Use string resource for format
                product.price
            )

            // Get category name
            val category = DataSource.getCategoryById(product.categoryId)
            binding.textProductCategory.text = category?.name ?: "Unknown Category"

            // Set placeholder image or load actual image if URL available
            // binding.imageProduct.load(product.imageUrl) // Using Coil or Glide library later

            // Handle click on the whole item
            binding.root.setOnClickListener {
                onProductClick(product)
            }

            // Handle click on the add button
            binding.buttonAddToBasket.setOnClickListener {
                onAddToBasketClick(product)
                // Optional: Visually indicate it was added (e.g., change icon briefly)
                // This button doesn't change state permanently here, detail view does.
            }

            // Example: Visually disable button if already in basket (requires access to basket state)
            // This logic is better handled in ProductDetail or Basket screen,
            // but could be done here if the Adapter has access to the basket state.
            // binding.buttonAddToBasket.isEnabled = !isProductInBasket(product.id)
        }
    }

    // DiffUtil for efficient list updates
    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}