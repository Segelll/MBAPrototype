package com.example.mbaprototype.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object InteractionLogger {

    private const val FILE_NAME = "etkilesim.csv"
    private const val CSV_HEADER = "userno,etkileşim türü,ürünid,tarih\n"

    // For simplicity, generate a unique ID per app installation if not available.
    // In a real app with user accounts, you'd use the actual user ID.
    private var userNo: String? = null
    private const val PREF_USER_NO = "pref_user_no"
    private const val KEY_USER_NO = "key_app_user_no"


    fun initialize(context: Context) {
        if (userNo == null) {
            val sharedPreferences = context.getSharedPreferences(PREF_USER_NO, Context.MODE_PRIVATE)
            var storedUserNo = sharedPreferences.getString(KEY_USER_NO, null)
            if (storedUserNo == null) {
                storedUserNo = UUID.randomUUID().toString()
                sharedPreferences.edit().putString(KEY_USER_NO, storedUserNo).apply()
            }
            userNo = storedUserNo
        }
        ensureHeader(context)
    }


    private fun ensureHeader(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            try {
                file.outputStream().use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        writer.append(CSV_HEADER)
                    }
                }
            } catch (e: IOException) {
                Log.e("InteractionLogger", "Error writing CSV header", e)
            }
        }
    }

    fun logInteraction(
        context: Context,
        interactionType: String,
        productId: String? // Can be product ID, search query, or other relevant ID
    ) {
        if (userNo == null) {
            Log.e("InteractionLogger", "UserNo not initialized. Call initialize() first.")
            // Attempt to initialize again as a fallback, though it should be done on app start
            initialize(context.applicationContext)
            if (userNo == null) return // Still null, cannot log
        }

        val file = File(context.filesDir, FILE_NAME)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        // Sanitize CSV data: remove commas and newlines from productId if it's a search query
        val sanitizedProductId = productId?.replace(",", "")?.replace("\n", " ") ?: ""

        val csvRow = "$userNo,$interactionType,$sanitizedProductId,$timestamp\n"

        try {
            // Append mode
            FileOutputStream(file, true).use { fos ->
                OutputStreamWriter(fos).use { writer ->
                    writer.append(csvRow)
                }
            }
        } catch (e: IOException) {
            Log.e("InteractionLogger", "Error writing to CSV", e)
        }
    }

    // Interaction type constants
    object InteractionType {
        const val PRODUCT_CLICK = "product_click"
        const val ADD_FAVORITE = "add_favorite"
        const val REMOVE_FAVORITE = "remove_favorite"
        const val SEARCH = "search"
        const val PURCHASED_ITEM = "purchased_item" // For logging items from a completed basket
    }
}