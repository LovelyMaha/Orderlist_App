package com.example.orderlistapp.data.model

object ItemNormalizer {
    fun normalize(itemString: String): String {
        val rawName = itemString.replace(Regex("\\d+.*$"), "").trim()
        return rawName.ifEmpty { "Unknown Item" }
    }
}
