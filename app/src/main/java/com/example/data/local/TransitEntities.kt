package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pois")
data class PoiEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val alternativeNames: List<String>,
    val localizedNames: Map<String, String>,
    val longitude: Double,
    val latitude: Double,
    val address: String?,
    val type: String?
)

@Entity(tableName = "streets")
data class StreetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val alternativeNames: List<String>,
    val coordinates: List<List<Double>>,
    val region: String?
)

@Entity(tableName = "street_junctions")
data class StreetJunctionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val streetRef: String,
    val longitude: Double,
    val latitude: Double
)

@Entity(tableName = "vip_cache")
data class CachedVipProfileEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String,
    val isVip: Boolean,
    val isAdmin: Boolean,
    val statusMessage: String,
    val lastCheckedTimestamp: Long
)
