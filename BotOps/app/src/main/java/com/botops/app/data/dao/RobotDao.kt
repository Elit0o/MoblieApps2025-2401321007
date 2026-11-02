package com.botops.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.RobotWithZone
import kotlinx.coroutines.flow.Flow

@Dao
interface RobotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRobot(robot: RobotEntity): Long

    @Query("SELECT * FROM robots ORDER BY robotName COLLATE NOCASE ASC")
    fun observeAllRobots(): Flow<List<RobotEntity>>

    @Query("""
        SELECT robots.*, zones.name AS zoneName 
        FROM robots 
        LEFT JOIN zones ON robots.zoneId = zones.id 
        ORDER BY robots.robotName COLLATE NOCASE ASC
    """)
    fun observeRobotsWithZone(): Flow<List<RobotWithZone>>

    @Query("SELECT * FROM robots WHERE id = :robotId")
    suspend fun getRobotById(robotId: Long): RobotEntity?

    @Query("DELETE FROM robots WHERE id = :robotId")
    suspend fun deleteRobotById(robotId: Long)
}
