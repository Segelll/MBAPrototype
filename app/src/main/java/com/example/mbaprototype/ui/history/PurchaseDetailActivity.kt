package com.example.mbaprototype.ui.history

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.PurchaseHistory
import com.example.mbaprototype.databinding.ActivityPurchaseDetailBinding // Assumes layout is activity_purchase_detail.xml
import com.example.mbaprototype.ui.SharedViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PurchaseDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PURCHASE_ID = "extra_purchase_id"
    }

    private lateinit var binding: ActivityPurchaseDetailBinding
    private val sharedViewModel: SharedViewModel by lazy {
        (application as MBAPrototypeApplication).sharedViewModel
    }
    private lateinit var purchaseDetailAdapter: PurchaseDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This will inflate R.layout.activity_purchase_detail
        binding = ActivityPurchaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarPurchaseDetail) // Expects @+id/toolbar_purchase_detail
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_purchase_detail)

        val purchaseId = intent.getStringExtra(EXTRA_PURCHASE_ID)

        setupRecyclerView()

        if (purchaseId == null) {
            binding.textPurchaseDetailError.text = getString(R.string.error_purchase_id_missing) // Expects @+id/text_purchase_detail_error
            binding.textPurchaseDetailError.isVisible = true
            binding.recyclerViewPurchaseItems.isVisible = false // Expects @+id/recycler_view_purchase_items
            binding.textPurchaseDate.isVisible = false // Expects @+id/text_purchase_date
            binding.layoutPurchaseContent.isVisible = false // Hide content group
            return
        } else {
            binding.layoutPurchaseContent.isVisible = true
        }

        lifecycleScope.launch {
            val purchaseHistory = sharedViewModel.getPurchaseById(purchaseId)
            if (purchaseHistory == null) {
                binding.textPurchaseDetailError.text = getString(R.string.error_purchase_not_found)
                binding.textPurchaseDetailError.isVisible = true
                binding.recyclerViewPurchaseItems.isVisible = false
                binding.textPurchaseDate.isVisible = false
                binding.layoutPurchaseContent.isVisible = false // Hide content group on error
            } else {
                binding.layoutPurchaseContent.isVisible = true
                populateUI(purchaseHistory)
            }
        }
    }

    private fun setupRecyclerView() {
        purchaseDetailAdapter = PurchaseDetailAdapter()
        binding.recyclerViewPurchaseItems.apply { // Expects @+id/recycler_view_purchase_items
            layoutManager = LinearLayoutManager(this@PurchaseDetailActivity)
            adapter = purchaseDetailAdapter
        }
    }

    private fun populateUI(purchase: PurchaseHistory) {
        binding.textPurchaseDetailError.isVisible = false
        binding.recyclerViewPurchaseItems.isVisible = true
        binding.textPurchaseDate.isVisible = true

        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        binding.textPurchaseDate.text = getString(R.string.purchase_date_prefix, dateFormat.format(purchase.purchaseDate)) // Expects @+id/text_purchase_date
        purchaseDetailAdapter.submitList(purchase.items)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}