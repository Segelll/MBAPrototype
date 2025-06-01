package com.example.mbaprototype.ui.products

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.ItemCategoryHeaderBinding
import com.example.mbaprototype.databinding.ItemCategorySelectorBinding
import com.example.mbaprototype.databinding.ItemProductBinding
import com.example.mbaprototype.ui.ProductListItem
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.utils.CategoryColorUtil
import com.google.android.material.chip.Chip

typealias OnProductClick = (Product) -> Unit
typealias OnAddToBasketClick = (Product) -> Unit
typealias OnCategoryHeaderClick = (Category) -> Unit
typealias OnCategoryChipSelected = (categoryId: String?) -> Unit

internal const val VIEW_TYPE_CATEGORY_SELECTOR = 0
internal const val VIEW_TYPE_HEADER = 1
internal const val VIEW_TYPE_PRODUCT = 2

class ProductAdapter(
    private val onProductClick: OnProductClick,
    private val onAddToBasketClick: OnAddToBasketClick,
    private val onCategoryHeaderClick: OnCategoryHeaderClick,
    private val onCategoryChipSelected: OnCategoryChipSelected,
    private val sharedViewModel: SharedViewModel
) : ListAdapter<ProductListItem, RecyclerView.ViewHolder>(ProductListDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductListItem.CategorySelectorItem -> VIEW_TYPE_CATEGORY_SELECTOR
            is ProductListItem.CategoryHeader -> VIEW_TYPE_HEADER
            is ProductListItem.ProductItem -> VIEW_TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY_SELECTOR -> CategorySelectorViewHolder.from(parent, onCategoryChipSelected)
            VIEW_TYPE_HEADER -> CategoryHeaderViewHolder.from(parent, onCategoryHeaderClick)
            VIEW_TYPE_PRODUCT -> ProductViewHolder.from(parent, onProductClick, onAddToBasketClick, sharedViewModel)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategorySelectorViewHolder -> {
                val item = getItem(position) as ProductListItem.CategorySelectorItem
                holder.bind(item)
            }
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

    class CategorySelectorViewHolder private constructor(
        private val binding: ItemCategorySelectorBinding,
        private val onCategoryChipSelected: OnCategoryChipSelected
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductListItem.CategorySelectorItem) {
            binding.chipGroupCategorySelector.removeAllViews()

            val allChip = Chip(binding.root.context).apply {
                text = context.getString(R.string.all_categories)
                isCheckable = true
                isChecked = item.selectedCategoryId == null
                setOnClickListener { onCategoryChipSelected(null) }
            }
            binding.chipGroupCategorySelector.addView(allChip)

            item.categories.forEach { category ->
                val chip = Chip(binding.root.context).apply {
                    text = category.name
                    tag = category.id
                    isCheckable = true
                    isChecked = item.selectedCategoryId == category.id
                    setOnClickListener { onCategoryChipSelected(category.id) }
                }
                binding.chipGroupCategorySelector.addView(chip)
            }
        }

        companion object {
            fun from(parent: ViewGroup, onCategoryChipSelected: OnCategoryChipSelected): CategorySelectorViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemCategorySelectorBinding.inflate(layoutInflater, parent, false)
                return CategorySelectorViewHolder(binding, onCategoryChipSelected)
            }
        }
    }


    class CategoryHeaderViewHolder private constructor(
        private val binding: ItemCategoryHeaderBinding,
        private val onCategoryHeaderClick: OnCategoryHeaderClick
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(item: ProductListItem.CategoryHeader) {
            binding.textCategoryName.text = item.category.name
            if (!item.isShowingAll) {
                binding.textSeeAll.visibility = View.VISIBLE
                binding.categoryHeaderRoot.setOnClickListener {
                    onCategoryHeaderClick(item.category)
                }
                binding.textSeeAll.setOnClickListener {
                    onCategoryHeaderClick(item.category)
                }
            } else {
                binding.textSeeAll.visibility = View.GONE
                binding.categoryHeaderRoot.setOnClickListener(null)
                binding.textSeeAll.setOnClickListener(null)
            }
        }
        companion object {
            fun from(parent: ViewGroup, onCategoryHeaderClick: OnCategoryHeaderClick): CategoryHeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemCategoryHeaderBinding.inflate(layoutInflater, parent, false)
                return CategoryHeaderViewHolder(binding, onCategoryHeaderClick)
            }
        }
    }

    class ProductViewHolder private constructor(
        private val binding: ItemProductBinding,
        private val onProductClick: OnProductClick,
        private val onAddToBasketClick: OnAddToBasketClick,
        private val sharedViewModel: SharedViewModel
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(product: Product) {
            binding.textProductName.text = product.name
            val category = sharedViewModel.getCategoryById(product.categoryId)
            binding.textProductCategory.text = category?.name ?: itemView.context.getString(R.string.unknown_category)
            binding.textProductCategory.visibility = View.VISIBLE

            val (backgroundColor, textColor) = CategoryColorUtil.getColorsForCategory(product.categoryId)
            binding.imageProductBackground.setImageDrawable(ColorDrawable(backgroundColor))

            val words = product.name.split(" ")
            var textToDisplay = ""

            if (words.isNotEmpty()) {
                val firstWord = words[0]
                // Check if the first word consists only of digits
                if (firstWord.isNotBlank() && firstWord.all { it.isDigit() }) {
                    textToDisplay = if (words.size > 1) {
                        "${words[0]} ${words[1]}".take(15) // Take first two words, limit combined length
                    } else {
                        firstWord.take(10) // Only one word (a number), limit length
                    }
                } else {
                    textToDisplay = firstWord.take(10) // First word is not a number (or empty), limit length
                }
            }

            binding.textProductFirstWord.text = textToDisplay.uppercase()
            binding.textProductFirstWord.setTextColor(textColor)

            binding.root.setOnClickListener { onProductClick(product) }
            binding.buttonAddToBasket.setOnClickListener { onAddToBasketClick(product) }
        }

        companion object {
            fun from(parent: ViewGroup, onProductClick: OnProductClick, onAddToBasketClick: OnAddToBasketClick, sharedViewModel: SharedViewModel): ProductViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemProductBinding.inflate(layoutInflater, parent, false)
                return ProductViewHolder(binding, onProductClick, onAddToBasketClick, sharedViewModel)
            }
        }
    }

    class ProductListDiffCallback : DiffUtil.ItemCallback<ProductListItem>() {
        override fun areItemsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
            return when {
                oldItem is ProductListItem.CategorySelectorItem && newItem is ProductListItem.CategorySelectorItem ->
                    oldItem.selectedCategoryId == newItem.selectedCategoryId && oldItem.categories.size == newItem.categories.size
                oldItem is ProductListItem.CategoryHeader && newItem is ProductListItem.CategoryHeader ->
                    oldItem.category.id == newItem.category.id && oldItem.isShowingAll == newItem.isShowingAll
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