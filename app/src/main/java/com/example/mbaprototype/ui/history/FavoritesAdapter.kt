package com.example.mbaprototype.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.ItemFavoriteBinding

typealias OnRemoveFavoriteClick = (String) -> Unit

class FavoritesAdapter(
    private val onRemoveFavoriteClick: OnRemoveFavoriteClick,
    private val onFavoriteClick: (Product) -> Unit
) : ListAdapter<Product, FavoritesAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoriteViewHolder(binding, onRemoveFavoriteClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FavoriteViewHolder(
        private val binding: ItemFavoriteBinding,
        private val onRemoveFavoriteClick: OnRemoveFavoriteClick,
        private val onFavoriteClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.textFavoriteName.text = product.name

            binding.buttonRemoveFavorite.setOnClickListener {
                onRemoveFavoriteClick(product.id)
            }
            binding.root.setOnClickListener {
                onFavoriteClick(product)
            }
        }
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}