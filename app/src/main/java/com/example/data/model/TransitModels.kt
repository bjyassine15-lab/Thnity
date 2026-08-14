package com.example.data.model

import com.google.gson.annotations.SerializedName

data class TransitDataset(
    @SerializedName("_version")
    val version: String = "3.1",
    @SerializedName("_fields")
    val fields: Map<String, List<String>> = emptyMap(),
    @SerializedName("pois")
    val pois: List<Poi> = emptyList(),
    @SerializedName("streets")
    val streets: List<Street> = emptyList(),
    @SerializedName("streetJunctions")
    val streetJunctions: List<StreetJunction> = emptyList()
)

data class Poi(
    val name: String,
    val alternativeNames: List<String> = emptyList(),
    val localizedNames: Map<String, String> = emptyMap(),
    val coordinates: List<Double> = emptyList(), // [lon, lat]
    val address: String? = null,
    val type: String? = null
) {
    val longitude: Double get() = coordinates.getOrNull(0) ?: 0.0
    val latitude: Double get() = coordinates.getOrNull(1) ?: 0.0

    fun getDisplayName(preferredLang: String = "ar"): String {
        return localizedNames[preferredLang] ?: name
    }
}

data class Street(
    val name: String,
    val alternativeNames: List<String> = emptyList(),
    val coordinates: List<List<Double>> = emptyList(), // [[lon, lat], [lon, lat], ...]
    val region: String? = null
)

data class StreetJunction(
    val streetRef: String,
    val coordinates: List<Double> = emptyList() // [lon, lat]
) {
    val longitude: Double get() = coordinates.getOrNull(0) ?: 0.0
    val latitude: Double get() = coordinates.getOrNull(1) ?: 0.0
}

data class UserVipProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val isVip: Boolean = false,
    val isAdmin: Boolean = false,
    val statusMessage: String = "في انتظار موافقة مسؤول النظام",
    val registeredAt: Long = System.currentTimeMillis(),
    val vipActivatedAt: Long? = null
)
