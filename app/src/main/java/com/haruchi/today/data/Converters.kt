package com.haruchi.today.data

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room type converters. Photos are stored as a compact JSON array on the receipt row
 * rather than a separate table — the PRD deliberately avoids over-normalization, and a
 * session's photo list is a small, ordered value with no independent identity.
 */
class Converters {

    @TypeConverter
    fun photosToJson(photos: List<PhotoRef>): String {
        val array = JSONArray()
        photos.forEach { p ->
            val obj = JSONObject()
            if (p.fileName != null) obj.put("f", p.fileName)
            obj.put("a", p.aspect.toDouble())
            obj.put("t", p.toneIndex)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToPhotos(json: String?): List<PhotoRef> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PhotoRef(
                    fileName = if (obj.has("f")) obj.getString("f") else null,
                    aspect = obj.optDouble("a", 1.0).toFloat(),
                    toneIndex = obj.optInt("t", 0),
                )
            }
        }.getOrDefault(emptyList())
    }
}
