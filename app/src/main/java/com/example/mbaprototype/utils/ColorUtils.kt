// You can put this in a new file, e.g., utils/ColorUtils.kt
package com.example.mbaprototype.utils

import android.graphics.Color

object CategoryColorUtil {

    // Define some color pairs (background, text)
    // You should expand this list or make it more dynamic
    private val colorPairs = listOf(
        Pair(Color.parseColor("#E8F5E9"), Color.parseColor("#1B5E20")), // Light Green BG, Dark Green Text
        Pair(Color.parseColor("#E3F2FD"), Color.parseColor("#0D47A1")), // Light Blue BG, Dark Blue Text
        Pair(Color.parseColor("#FFFDE7"), Color.parseColor("#F57F17")), // Light Yellow BG, Dark Yellow/Orange Text
        Pair(Color.parseColor("#FCE4EC"), Color.parseColor("#880E4F")), // Light Pink BG, Dark Pink/Magenta Text
        Pair(Color.parseColor("#EDE7F6"), Color.parseColor("#311B92")), // Light Purple BG, Dark Purple Text
        Pair(Color.parseColor("#E0F2F1"), Color.parseColor("#004D40"))  // Light Teal BG, Dark Teal Text
    )

    private val defaultColorPair = Pair(Color.parseColor("#ECEFF1"), Color.parseColor("#263238")) // Light Gray BG, Dark Gray Text

    private val categoryColorMap = mutableMapOf<String, Pair<Int, Int>>()
    private var nextColorIndex = 0

    fun getColorsForCategory(categoryId: String): Pair<Int, Int> {
        return categoryColorMap.getOrPut(categoryId) {
            val pair = colorPairs.getOrElse(nextColorIndex % colorPairs.size) { defaultColorPair }
            nextColorIndex++
            pair
        }
    }
}