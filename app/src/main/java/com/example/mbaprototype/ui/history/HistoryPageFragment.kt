package com.example.mbaprototype.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.databinding.PageHistoryBinding
import com.example.mbaprototype.ui.SharedViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
// This import should now work if PurchaseDetailActivity.kt is fixed
import com.example.mbaprototype.ui.history.PurchaseDetailActivity

class HistoryPageFragment : Fragment() { // This is the only declaration of HistoryPageFragment
    private var _binding: PageHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter
    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity().application as MBAPrototypeApplication).sharedViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PageHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { purchaseHistory ->
            val intent = Intent(activity, PurchaseDetailActivity::class.java).apply {
                putExtra(PurchaseDetailActivity.EXTRA_PURCHASE_ID, purchaseHistory.purchaseId)
            }
            startActivity(intent)
        }
        binding.recyclerViewHistoryPage.apply { // Expects @+id/recycler_view_history_page in page_history.xml
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.pastPurchases.collectLatest { purchases ->
                historyAdapter.submitList(purchases)
                // Ensure text_empty_history_page and recycler_view_history_page IDs exist in page_history.xml
                binding.textEmptyHistoryPage.isVisible = purchases.isEmpty()
                binding.recyclerViewHistoryPage.isVisible = purchases.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewHistoryPage.adapter = null
        _binding = null
    }
}