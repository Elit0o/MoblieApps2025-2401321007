package com.botops.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.WaypointWithZone
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long

    @Update
    suspend fun updateWaypoint(waypoint: WaypointEntity)

    @Query("SELECT * FROM waypoints ORDER BY name COLLATE NOCASE ASC")
    fun observeAllWaypoints(): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints")
    suspend fun getAllWaypointsSync(): List<WaypointEntity>

    @Query("""
        SELECT waypoints.*, zones.name AS zoneName 
        FROM waypoints 
        LEFT JOIN zones ON waypoints.zoneId = zones.id 
        ORDER BY waypoints.name COLLATE NOCASE ASC
    """)
    fun observeWaypointsWithZone(): Flow<List<WaypointWithZone>>

    @Query("SELECT * FROM waypoints WHERE zoneId = :zoneId ORDER BY name COLLATE NOCASE ASC")
    fun observeWaypointsByZone(zoneId: Long): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE id = :waypointId")
    suspend fun getWaypointById(waypointId: Long): WaypointEntity?

    @Query("DELETE FROM waypoints WHERE id = :waypointId")
    suspend fun deleteWaypointById(waypointId: Long)
}
