package com.example.mbaprototype.ui.basket

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.databinding.ItemBasketBinding

typealias OnRemoveFromBasketClick = (productId: String) -> Unit
typealias OnIncreaseQuantityClick = (productId: String) -> Unit
typealias OnDecreaseQuantityClick = (productId: String) -> Unit

class BasketAdapter(
    private val onRemoveClick: OnRemoveFromBasketClick,
    private val onIncreaseClick: OnIncreaseQuantityClick,
    private val onDecreaseClick: OnDecreaseQuantityClick
) : ListAdapter<BasketItem, BasketAdapter.BasketViewHolder>(BasketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasketViewHolder {
        val binding = ItemBasketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BasketViewHolder(binding, onRemoveClick, onIncreaseClick, onDecreaseClick)
    }

    override fun onBindViewHolder(holder: BasketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BasketViewHolder(
        private val binding: ItemBasketBinding,
        private val onRemoveClick: OnRemoveFromBasketClick,
        private val onIncreaseClick: OnIncreaseQuantityClick,
        private val onDecreaseClick: OnDecreaseQuantityClick
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(basketItem: BasketItem) {
            binding.textBasketItemName.text = basketItem.product.name
            binding.textBasketItemQuantityValue.text = basketItem.quantity.toString()

            binding.buttonRemoveFromBasket.setOnClickListener {
                onRemoveClick(basketItem.product.id)
            }
            binding.buttonIncreaseQuantityBasket.setOnClickListener {
                onIncreaseClick(basketItem.product.id)
            }
            // Disable decrease if quantity is 0, though ViewModel should prevent this state for active items.
            binding.buttonDecreaseQuantityBasket.isEnabled = basketItem.quantity > 0

            // If quantity is 1, the decrease button should trigger a remove action.
            // Otherwise, it triggers a decrease action.
            if (basketItem.quantity <= 1) {
                binding.buttonDecreaseQuantityBasket.setImageResource(R.drawable.ic_delete) // Change icon to delete
                binding.buttonDecreaseQuantityBasket.setOnClickListener {
                    onRemoveClick(basketItem.product.id)
                }
            } else {
                binding.buttonDecreaseQuantityBasket.setImageResource(R.drawable.ic_remove) // Standard remove icon
                binding.buttonDecreaseQuantityBasket.setOnClickListener {
                    onDecreaseClick(basketItem.product.id)
                }
            }
        }
    }

    class BasketDiffCallback : DiffUtil.ItemCallback<BasketItem>() {
        override fun areItemsTheSame(oldItem: BasketItem, newItem: BasketItem): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: BasketItem, newItem: BasketItem): Boolean {
            return oldItem == newItem // Compares quantity as well due to data class
        }
    }
}