package com.example.data.util

import org.json.JSONArray
import org.json.JSONObject

object JsonUtils {
    fun stringListToJson(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    fun jsonToStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun escapeJson(text: String): String {
        return JSONObject.quote(text)
    }
}
