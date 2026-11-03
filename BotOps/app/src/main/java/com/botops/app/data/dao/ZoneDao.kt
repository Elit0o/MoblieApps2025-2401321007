package com.botops.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.botops.app.data.entities.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: ZoneEntity): Long

    @Update
    suspend fun updateZone(zone: ZoneEntity)

    @Query("SELECT * FROM zones ORDER BY name COLLATE NOCASE ASC")
    fun observeAllZones(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones")
    suspend fun getAllZonesSync(): List<ZoneEntity>

    @Query("SELECT * FROM zones WHERE id = :zoneId")
    suspend fun getZoneById(zoneId: Long): ZoneEntity?

    @Query("DELETE FROM zones WHERE id = :zoneId")
    suspend fun deleteZoneById(zoneId: Long)
}
