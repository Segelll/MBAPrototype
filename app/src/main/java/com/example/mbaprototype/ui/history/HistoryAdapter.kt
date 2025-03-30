package com.example.mbaprototype.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.PurchaseHistory
import com.example.mbaprototype.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter : ListAdapter<PurchaseHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding, dateFormat)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ItemHistoryBinding,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: PurchaseHistory) {
            binding.textHistoryDate.text = itemView.context.getString(
                R.string.history_date_prefix,
                dateFormat.format(history.purchaseDate)
            )

            val itemNames = history.items.joinToString(", ") { it.name }
            binding.textHistoryItems.text = itemView.context.getString(
                R.string.history_items_prefix,
                itemNames
            )
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<PurchaseHistory>() {
        override fun areItemsTheSame(oldItem: PurchaseHistory, newItem: PurchaseHistory): Boolean {
            return oldItem.purchaseId == newItem.purchaseId
        }

        override fun areContentsTheSame(oldItem: PurchaseHistory, newItem: PurchaseHistory): Boolean {
            return oldItem == newItem
        }
    }
}