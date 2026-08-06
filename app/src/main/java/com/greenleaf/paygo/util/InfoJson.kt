package com.greenleaf.paygo.util

import org.json.JSONObject

/** Serialises/parses the captured-info map stored on a message. */
object InfoJson {

    fun encode(map: Map<String, String>): String? =
        if (map.isEmpty()) null else JSONObject(map as Map<*, *>).toString()

    fun decode(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.optString(it) }
                .filterValues { it.isNotBlank() }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
