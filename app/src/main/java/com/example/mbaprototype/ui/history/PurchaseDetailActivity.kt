package com.example.mbaprototype.ui.history

import android.icu.text.SimpleDateFormat // Use ICU version for more robust date/time formatting
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.PurchaseHistory
import com.example.mbaprototype.databinding.ActivityPurchaseDetailBinding
import com.example.mbaprototype.ui.SharedViewModel
import java.util.Locale

class PurchaseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPurchaseDetailBinding
    private val viewModel: SharedViewModel by lazy {
        (application as MBAPrototypeApplication).sharedViewModel
    }
    private lateinit var detailAdapter: PurchaseDetailAdapter

    companion object {
        const val EXTRA_PURCHASE_ID = "extra_purchase_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarPurchaseDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Title is already set in XML, but can be overridden if needed
        // supportActionBar?.title = getString(R.string.purchase_details)

        val purchaseId = intent.getStringExtra(EXTRA_PURCHASE_ID)
        Log.d("PurchaseDetailActivity", "Received Purchase ID: $purchaseId")

        if (purchaseId.isNullOrBlank()) {
            Log.e("PurchaseDetailActivity", "Error: Purchase ID received is null or blank.")
            showErrorAndFinish()
            return
        }

        Log.d("PurchaseDetailActivity", "Calling viewModel.getPurchaseById for ID: $purchaseId")
        val purchaseHistory = viewModel.getPurchaseById(purchaseId)

        if (purchaseHistory == null) {
            Log.e("PurchaseDetailActivity", "ViewModel returned null for PurchaseHistory ID: $purchaseId")
            Log.d("PurchaseDetailActivity", "Current purchase history size in VM: ${viewModel.pastPurchases.value.size}")
            viewModel.pastPurchases.value.forEachIndexed { index, history ->
                Log.d("PurchaseDetailActivity", "VM History[$index]: ID=${history.purchaseId}")
            }
            showErrorAndFinish()
            return
        }

        Log.d("PurchaseDetailActivity", "Successfully found PurchaseHistory: $purchaseHistory")
        setupRecyclerView()
        populateUI(purchaseHistory)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun showErrorAndFinish() {
        Toast.makeText(this, R.string.error_purchase_not_found, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun setupRecyclerView() {
        detailAdapter = PurchaseDetailAdapter()
        binding.recyclerViewPurchaseItems.apply {
            adapter = detailAdapter
            layoutManager = LinearLayoutManager(this@PurchaseDetailActivity)
            // itemAnimator = DefaultItemAnimator() // Optional: for default animations
        }
    }

    private fun populateUI(history: PurchaseHistory) {
        // Using ICU SimpleDateFormat for potentially better locale handling and more pattern options
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

        binding.textPurchaseDetailDate.text = getString(R.string.purchase_date_header, dateFormat.format(history.purchaseDate))
        // Total cost is no longer displayed
        // binding.textPurchaseDetailTotal.text = getString(R.string.purchase_total_cost, getString(R.string.price_format, history.totalCost))
        binding.textPurchaseDetailItemCount.text = getString(R.string.purchase_item_count_details, history.items.sumOf { it.quantity })


        detailAdapter.submitList(history.items)
        binding.recyclerViewPurchaseItems.isVisible = history.items.isNotEmpty()
    }
}