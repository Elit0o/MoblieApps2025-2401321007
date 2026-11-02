package com.botops.app.data.repository

import com.botops.app.data.dao.RobotDao
import com.botops.app.data.dao.TaskDao
import com.botops.app.data.dao.WaypointDao
import com.botops.app.data.dao.ZoneDao
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.ZoneEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class BotOpsRepository(
    private val zoneDao: ZoneDao,
    private val waypointDao: WaypointDao,
    private val robotDao: RobotDao,
    private val taskDao: TaskDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    // --- ZONES ---
    suspend fun insertZone(zone: ZoneEntity): Long {
        // 1. Записваме локално в Room
        val localId = zoneDao.insertZone(zone)

        // 2. Синхронизираме към Firebase Firestore
        val zoneData = mapOf(
            "id" to localId,
            "name" to zone.name,
            "description" to zone.description
        )
        firestore.collection("zones").document(localId.toString()).set(zoneData)

        return localId
    }

    val allZones: Flow<List<ZoneEntity>> = zoneDao.observeAllZones()

    suspend fun deleteZone(id: Long) {
        zoneDao.deleteZoneById(id)
        firestore.collection("zones").document(id.toString()).delete()
    }

    // --- WAYPOINTS ---
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long {
        val localId = waypointDao.insertWaypoint(waypoint)

        val waypointData = mapOf(
            "id" to localId,
            "name" to waypoint.name,
            "xCord" to waypoint.xCord,
            "yCord" to waypoint.yCord,
            "zoneId" to waypoint.zoneId
        )
        firestore.collection("waypoints").document(localId.toString()).set(waypointData)

        return localId
    }

    val allWaypoints: Flow<List<WaypointEntity>> = waypointDao.observeAllWaypoints()
    fun getWaypointsByZone(zoneId: Long): Flow<List<WaypointEntity>> = waypointDao.observeWaypointsByZone(zoneId)

    suspend fun deleteWaypoint(id: Long) {
        waypointDao.deleteWaypointById(id)
        firestore.collection("waypoints").document(id.toString()).delete()
    }

    // --- ROBOTS ---
    suspend fun insertRobot(robot: RobotEntity): Long {
        val localId = robotDao.insertRobot(robot)

        val robotData = mapOf(
            "id" to localId,
            "robotName" to robot.robotName,
            "robotNumber" to robot.robotNumber,
            "status" to robot.status,
            "batteryLevel" to robot.batteryLevel,
            "zoneId" to robot.zoneId
        )
        firestore.collection("robots").document(localId.toString()).set(robotData)

        return localId
    }

    val allRobots: Flow<List<RobotEntity>> = robotDao.observeAllRobots()

    suspend fun deleteRobot(id: Long) {
        robotDao.deleteRobotById(id)
        firestore.collection("robots").document(id.toString()).delete()
    }

    // --- TASKS ---
    suspend fun insertTask(task: TaskEntity): Long {
        val localId = taskDao.insertTask(task)

        val taskData = mapOf(
            "id" to localId,
            "robotId" to task.robotId,
            "waypointId" to task.waypointId,
            "status" to task.status,
            "priority" to task.priority,
            "createdAt" to task.createdAt
        )
        firestore.collection("tasks").document(localId.toString()).set(taskData)

        return localId
    }

    val allTasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    fun getTasksByRobot(robotId: Long): Flow<List<TaskEntity>> = taskDao.observeTasksByRobot(robotId)

    suspend fun deleteTask(id: Long) {
        taskDao.deleteTaskById(id)
        firestore.collection("tasks").document(id.toString()).delete()
    }
}