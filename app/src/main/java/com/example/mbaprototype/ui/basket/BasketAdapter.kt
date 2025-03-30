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

typealias OnRemoveFromBasketClick = (String) -> Unit

class BasketAdapter(
    private val onRemoveClick: OnRemoveFromBasketClick
) : ListAdapter<BasketItem, BasketAdapter.BasketViewHolder>(BasketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasketViewHolder {
        val binding = ItemBasketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BasketViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: BasketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BasketViewHolder(
        private val binding: ItemBasketBinding,
        private val onRemoveClick: OnRemoveFromBasketClick
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(basketItem: BasketItem) {
            binding.textBasketItemName.text = basketItem.product.name
            binding.textBasketItemPrice.text = String.format(
                itemView.context.getString(R.string.basket_item_format),
                String.format(itemView.context.getString(R.string.basket_item_price_format), basketItem.product.price),
                basketItem.quantity
            )

            binding.buttonRemoveFromBasket.setOnClickListener {
                onRemoveClick(basketItem.product.id)
            }
        }
    }

    class BasketDiffCallback : DiffUtil.ItemCallback<BasketItem>() {
        override fun areItemsTheSame(oldItem: BasketItem, newItem: BasketItem): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: BasketItem, newItem: BasketItem): Boolean {
            return oldItem == newItem
        }
    }
}