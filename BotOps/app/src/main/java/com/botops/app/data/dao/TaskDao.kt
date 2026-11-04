package com.botops.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.TaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, status: String)

    @Query("""
        SELECT * FROM tasks 
        ORDER BY CASE status 
            WHEN 'Active' THEN 1 
            WHEN 'Pending' THEN 2 
            WHEN 'Paused' THEN 3 
            WHEN 'Cancelled' THEN 4 
            WHEN 'Completed' THEN 5 
            ELSE 6 
        END, priority DESC, createdAt ASC
    """)
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("""
        SELECT tasks.*, 
               robots.robotName AS robotName, 
               robots.batteryLevel AS robotBattery,
               robots.status AS robotStatus,
               waypoints.name AS waypointName,
               zones.name AS zoneName
        FROM tasks 
        LEFT JOIN robots ON tasks.robotId = robots.id 
        LEFT JOIN waypoints ON tasks.waypointId = waypoints.id 
        LEFT JOIN zones ON waypoints.zoneId = zones.id
        ORDER BY CASE tasks.status 
            WHEN 'Active' THEN 1 
            WHEN 'Pending' THEN 2 
            WHEN 'Paused' THEN 3 
            WHEN 'Cancelled' THEN 4 
            WHEN 'Completed' THEN 5 
            ELSE 6 
        END, tasks.priority DESC, tasks.createdAt ASC
    """)
    fun observeTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Query("SELECT * FROM tasks WHERE robotId = :robotId ORDER BY priority DESC, createdAt ASC")
    fun observeTasksByRobot(robotId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE robotId = :robotId AND status != 'Completed' AND status != 'Cancelled' AND status != 'Paused' ORDER BY priority DESC, createdAt ASC")
    suspend fun getActiveAndPendingTasksForRobotSync(robotId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC")
    fun observeTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
}
