package com.example.mbaprototype.ui.history

import android.annotation.SuppressLint
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

typealias OnHistoryClick = (PurchaseHistory) -> Unit

class HistoryAdapter(
    private val onHistoryClick: OnHistoryClick
) : ListAdapter<PurchaseHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) // Corrected year pattern

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding, dateFormat, onHistoryClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ItemHistoryBinding,
        private val dateFormat: SimpleDateFormat,
        private val onHistoryClick: OnHistoryClick
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(history: PurchaseHistory) {
            binding.textHistoryDate.text = itemView.context.getString(
                R.string.history_date_prefix,
                dateFormat.format(history.purchaseDate)
            )

            val itemNames = history.items.joinToString(", ") { it.product.name }
            binding.textHistoryItemsSummary.text = itemView.context.getString(
                R.string.history_items_prefix,
                itemNames
            )

            // Total cost is no longer displayed
            // binding.textHistoryTotalCost.text = itemView.context.getString(
            // R.string.purchase_total_cost,
            // String.format(itemView.context.getString(R.string.price_format), history.totalCost)
            // )
            binding.textHistoryItemCount.text = itemView.context.getString(R.string.history_item_count, history.items.sumOf { it.quantity })


            binding.root.setOnClickListener {
                onHistoryClick(history)
            }
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