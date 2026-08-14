package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {
    @Query("SELECT * FROM pois ORDER BY name ASC")
    fun getAllPoisFlow(): Flow<List<PoiEntity>>

    @Query("SELECT * FROM pois WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    fun searchPoisFlow(query: String): Flow<List<PoiEntity>>

    @Query("SELECT COUNT(*) FROM pois")
    fun getPoiCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPois(pois: List<PoiEntity>)

    @Query("DELETE FROM pois")
    suspend fun clearAll()
}

@Dao
interface StreetDao {
    @Query("SELECT * FROM streets ORDER BY name ASC")
    fun getAllStreetsFlow(): Flow<List<StreetEntity>>

    @Query("SELECT COUNT(*) FROM streets")
    fun getStreetCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreets(streets: List<StreetEntity>)

    @Query("DELETE FROM streets")
    suspend fun clearAll()
}

@Dao
interface StreetJunctionDao {
    @Query("SELECT * FROM street_junctions ORDER BY streetRef ASC")
    fun getAllJunctionsFlow(): Flow<List<StreetJunctionEntity>>

    @Query("SELECT COUNT(*) FROM street_junctions")
    fun getJunctionCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJunctions(junctions: List<StreetJunctionEntity>)

    @Query("DELETE FROM street_junctions")
    suspend fun clearAll()
}

@Dao
interface VipCacheDao {
    @Query("SELECT * FROM vip_cache WHERE uid = :uid LIMIT 1")
    suspend fun getCachedProfile(uid: String): CachedVipProfileEntity?

    @Query("SELECT * FROM vip_cache WHERE uid = :uid LIMIT 1")
    fun getCachedProfileFlow(uid: String): Flow<CachedVipProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedProfile(profile: CachedVipProfileEntity)

    @Query("DELETE FROM vip_cache WHERE uid = :uid")
    suspend fun clearProfile(uid: String)
}
