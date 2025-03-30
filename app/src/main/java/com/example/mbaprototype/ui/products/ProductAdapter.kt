package com.example.mbaprototype.ui.products

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.ItemCategoryHeaderBinding
import com.example.mbaprototype.databinding.ItemProductBinding
import com.example.mbaprototype.ui.ProductListItem

typealias OnProductClick = (Product) -> Unit
typealias OnAddToBasketClick = (Product) -> Unit

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_PRODUCT = 1

class ProductAdapter(
    private val onProductClick: OnProductClick,
    private val onAddToBasketClick: OnAddToBasketClick
) : ListAdapter<ProductListItem, RecyclerView.ViewHolder>(ProductListDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductListItem.CategoryHeader -> VIEW_TYPE_HEADER
            is ProductListItem.ProductItem -> VIEW_TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> CategoryHeaderViewHolder.from(parent)
            VIEW_TYPE_PRODUCT -> ProductViewHolder.from(parent, onProductClick, onAddToBasketClick)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryHeaderViewHolder -> {
                val item = getItem(position) as ProductListItem.CategoryHeader
                holder.bind(item)
            }
            is ProductViewHolder -> {
                val item = getItem(position) as ProductListItem.ProductItem
                holder.bind(item.product)
            }
        }
    }

    class CategoryHeaderViewHolder private constructor(private val binding: ItemCategoryHeaderBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: ProductListItem.CategoryHeader) {
            binding.textCategoryName.text = item.category.name
        }
        companion object {
            fun from(parent: ViewGroup): CategoryHeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemCategoryHeaderBinding.inflate(layoutInflater, parent, false)
                return CategoryHeaderViewHolder(binding)
            }
        }
    }

    class ProductViewHolder private constructor(
        private val binding: ItemProductBinding,
        private val onProductClick: OnProductClick,
        private val onAddToBasketClick: OnAddToBasketClick
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(product: Product) {
            binding.textProductName.text = product.name
            binding.textProductPrice.text = String.format(
                itemView.context.getString(R.string.price_format),
                product.price
            )
            binding.root.setOnClickListener { onProductClick(product) }
            binding.buttonAddToBasket.setOnClickListener { onAddToBasketClick(product) }
            // binding.imageProduct.load(product.imageUrl) // Load image here
        }

        companion object {
            fun from(parent: ViewGroup, onProductClick: OnProductClick, onAddToBasketClick: OnAddToBasketClick): ProductViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemProductBinding.inflate(layoutInflater, parent, false)
                return ProductViewHolder(binding, onProductClick, onAddToBasketClick)
            }
        }
    }

    class ProductListDiffCallback : DiffUtil.ItemCallback<ProductListItem>() {
        override fun areItemsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
            return when {
                oldItem is ProductListItem.CategoryHeader && newItem is ProductListItem.CategoryHeader ->
                    oldItem.category.id == newItem.category.id
                oldItem is ProductListItem.ProductItem && newItem is ProductListItem.ProductItem ->
                    oldItem.product.id == newItem.product.id
                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
            return oldItem == newItem
        }
    }
}