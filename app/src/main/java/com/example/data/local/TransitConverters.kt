package com.example.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransitConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value ?: emptyMap<String, String>())
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromCoordinateList(value: List<List<Double>>?): String {
        return gson.toJson(value ?: emptyList<List<Double>>())
    }

    @TypeConverter
    fun toCoordinateList(value: String?): List<List<Double>> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<List<Double>>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
