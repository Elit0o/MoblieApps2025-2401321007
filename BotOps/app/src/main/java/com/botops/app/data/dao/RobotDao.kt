package com.botops.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.botops.app.data.entities.RobotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RobotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRobot(robot: RobotEntity): Long

    @Query("SELECT * FROM robots ORDER BY robotName COLLATE NOCASE ASC")
    fun observeAllRobots(): Flow<List<RobotEntity>>

    @Query("SELECT * FROM robots WHERE id = :robotId")
    fun getRobotById(robotId: Long): RobotEntity?

    @Query("DELETE FROM robots WHERE id = :robotId")
    suspend fun deleteRobotById(robotId: Long)
}