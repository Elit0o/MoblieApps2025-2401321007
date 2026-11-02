package com.botops.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.TaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT tasks.*, robots.robotName AS robotName, waypoints.name AS waypointName 
        FROM tasks 
        LEFT JOIN robots ON tasks.robotId = robots.id 
        LEFT JOIN waypoints ON tasks.waypointId = waypoints.id 
        ORDER BY tasks.createdAt DESC
    """)
    fun observeTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Query("SELECT * FROM tasks WHERE robotId = :robotId ORDER BY priority DESC")
    fun observeTasksByRobot(robotId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC")
    fun observeTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
}
