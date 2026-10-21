package com.botops.app.data.repository

import com.botops.app.data.dao.RobotDao
import com.botops.app.data.dao.TaskDao
import com.botops.app.data.dao.WaypointDao
import com.botops.app.data.dao.ZoneDao
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.ZoneEntity
import kotlinx.coroutines.flow.Flow

class BotOpsRepository(
    private val zoneDao: ZoneDao,
    private val waypointDao: WaypointDao,
    private val robotDao: RobotDao,
    private val taskDao: TaskDao   
) {

    suspend fun insertZone(zone: ZoneEntity): Long = zoneDao.insertZone(zone)
    val allZones: Flow<List<ZoneEntity>> = zoneDao.observeAllZones()
    suspend fun deleteZone(id: Long) = zoneDao.deleteZoneById(id)

    suspend fun insertWaypoint(waypoint: WaypointEntity): Long = waypointDao.insertWaypoint(waypoint)
    val allWaypoints: Flow<List<WaypointEntity>> = waypointDao.observeAllWaypoints()
    fun getWaypointsByZone(zoneId: Long): Flow<List<WaypointEntity>> = waypointDao.observeWaypointsByZone(zoneId)
    suspend fun deleteWaypoint(id: Long) = waypointDao.deleteWaypointById(id)

    suspend fun insertRobot(robot: RobotEntity): Long = robotDao.insertRobot(robot)
    val allRobots: Flow<List<RobotEntity>> = robotDao.observeAllRobots()
    suspend fun deleteRobot(id: Long) = robotDao.deleteRobotById(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    val allTasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    fun getTasksByRobot(robotId: Long): Flow<List<TaskEntity>> = taskDao.observeTasksByRobot(robotId)
    suspend fun deleteTask(id: Long) = taskDao.deleteTaskById(id)
}