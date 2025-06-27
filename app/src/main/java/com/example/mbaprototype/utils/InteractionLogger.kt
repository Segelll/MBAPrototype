package com.example.mbaprototype.utils

import android.util.Log
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.network.InteractionRequest
import com.example.mbaprototype.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InteractionLogger {

    private const val TAG = "InteractionLogger"

    fun logInteraction(product: Product, interactionType: String) {
        // Geçerli olmayan 'unfavorite' gibi etkileşimleri göndermeyi engelle
        val validInteractionTypes = listOf("favorites", "bought", "scan", "click")
        if (!validInteractionTypes.contains(interactionType)) {
            Log.w(TAG, "Geçersiz etkileşim türü gönderilmedi: $interactionType")
            return
        }

        // Ağ isteğini ana iş parçacığı dışında bir Coroutine içinde yap
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = InteractionRequest(
                    product_no = product.id,
                    interaction_type = interactionType
                )
                val response = RetrofitClient.instance.postInteraction(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Etkileşim başarıyla kaydedildi: ${response.body()}")
                } else {
                    Log.e(TAG, "Etkileşim kaydedilemedi: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Etkileşim kaydı sırasında istisna oluştu", e)
            }
        }
    }
}