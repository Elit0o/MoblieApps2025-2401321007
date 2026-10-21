package com.botops.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.botops.app.data.entities.WaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long

    @Query("SELECT * FROM waypoints ORDER BY name COLLATE NOCASE ASC")
    fun observeAllWaypoints(): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE zoneId = :zoneId ORDER BY name COLLATE NOCASE ASC")
    fun observeWaypointsByZone(zoneId: Long): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE id = :waypointId")
    suspend fun getWaypointById(waypointId: Long): WaypointEntity?

    @Query("DELETE FROM waypoints WHERE id = :waypointId")
    suspend fun deleteWaypointById(waypointId: Long)
}