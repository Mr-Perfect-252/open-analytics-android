package com.opensdk.analytics.internal

import org.json.JSONArray
import org.json.JSONObject

/** Minimal JSON helpers built on the framework-bundled org.json (zero deps). */
internal object JsonUtil {

    fun toJson(value: Any?): Any? = when (value) {
        null -> JSONObject.NULL
        is JSONObject, is JSONArray -> value
        is Map<*, *> -> mapToJson(value)
        is Collection<*> -> listToJson(value)
        is Array<*> -> listToJson(value.toList())
        is Number, is Boolean, is String -> value
        else -> value.toString()
    }

    fun mapToJson(map: Map<*, *>): JSONObject {
        val obj = JSONObject()
        for ((k, v) in map) {
            if (k == null) continue
            obj.put(k.toString(), toJson(v))
        }
        return obj
    }

    private fun listToJson(list: Collection<*>): JSONArray {
        val arr = JSONArray()
        for (v in list) arr.put(toJson(v))
        return arr
    }

    /** Returns the string at [key], or null if absent or JSON null (Kotlin-null-safe). */
    fun optStringOrNull(obj: JSONObject, key: String): String? =
        if (obj.has(key) && !obj.isNull(key)) obj.getString(key) else null

    fun jsonToMap(obj: JSONObject): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            out[key] = unwrap(obj.get(key))
        }
        return out
    }

    private fun unwrap(value: Any?): Any? = when (value) {
        JSONObject.NULL -> null
        is JSONObject -> jsonToMap(value)
        is JSONArray -> (0 until value.length()).map { unwrap(value.get(it)) }
        else -> value
    }
}
