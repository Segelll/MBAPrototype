package com.example.mbaprototype.ui.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.databinding.ItemPurchaseDetailItemBinding

class PurchaseDetailAdapter : ListAdapter<BasketItem, PurchaseDetailAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder private constructor(private val binding: ItemPurchaseDetailItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: BasketItem) {
            binding.textPurchaseItemName.text = item.product.name
            // Price per unit is no longer displayed
            // binding.textPurchaseItemPricePerUnit.text = String.format(
            //     itemView.context.getString(R.string.basket_item_price_format), // Re-use for format consistency if needed
            //     item.product.price
            // ) + " each"
            binding.textPurchaseItemQuantity.text = itemView.context.getString(R.string.quantity_prefix_details, item.quantity)
            // Line total is no longer displayed
            // binding.textPurchaseItemLineTotal.text = String.format(
            //     itemView.context.getString(R.string.price_format),
            //     item.product.price * item.quantity
            // )
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemPurchaseDetailItemBinding.inflate(layoutInflater, parent, false)
                return ItemViewHolder(binding)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BasketItem>() {
        override fun areItemsTheSame(oldItem: BasketItem, newItem: BasketItem): Boolean {
            // Assuming product.id is unique and stable
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: BasketItem, newItem: BasketItem): Boolean {
            return oldItem == newItem
        }
    }
}