package com.botops.app.data.repository

import com.botops.app.data.dao.RobotDao
import com.botops.app.data.dao.TaskDao
import com.botops.app.data.dao.WaypointDao
import com.botops.app.data.dao.ZoneDao
import com.botops.app.data.entities.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BotOpsRepository(
    private val zoneDao: ZoneDao,
    private val waypointDao: WaypointDao,
    private val robotDao: RobotDao,
    private val taskDao: TaskDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    // --- ZONES ---
    suspend fun insertZone(zone: ZoneEntity): Long = withContext(Dispatchers.IO) {
        val localId = zoneDao.insertZone(zone)
        val zoneData = mapOf(
            "id" to localId,
            "name" to zone.name,
            "description" to zone.description
        )
        try {
            firestore.collection("zones").document(localId.toString()).set(zoneData)
        } catch (_: Exception) {}
        localId
    }

    val allZones: Flow<List<ZoneEntity>> = zoneDao.observeAllZones()

    suspend fun deleteZone(id: Long) = withContext(Dispatchers.IO) {
        zoneDao.deleteZoneById(id)
        try {
            firestore.collection("zones").document(id.toString()).delete()
        } catch (_: Exception) {}
    }

    // --- WAYPOINTS ---
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long = withContext(Dispatchers.IO) {
        var targetZoneId = waypoint.zoneId
        val existingZone = zoneDao.getZoneById(targetZoneId)
        if (existingZone == null) {
            targetZoneId = zoneDao.insertZone(ZoneEntity(name = "Default Zone", description = "Auto-created zone"))
        }
        val adjustedWaypoint = waypoint.copy(zoneId = targetZoneId)
        val localId = waypointDao.insertWaypoint(adjustedWaypoint)

        val waypointData = mapOf(
            "id" to localId,
            "name" to adjustedWaypoint.name,
            "xCord" to adjustedWaypoint.xCord,
            "yCord" to adjustedWaypoint.yCord,
            "zoneId" to targetZoneId
        )
        try {
            firestore.collection("waypoints").document(localId.toString()).set(waypointData)
        } catch (_: Exception) {}

        localId
    }

    val allWaypoints: Flow<List<WaypointEntity>> = waypointDao.observeAllWaypoints()
    val waypointsWithZone: Flow<List<WaypointWithZone>> = waypointDao.observeWaypointsWithZone()
    fun getWaypointsByZone(zoneId: Long): Flow<List<WaypointEntity>> = waypointDao.observeWaypointsByZone(zoneId)

    suspend fun deleteWaypoint(id: Long) = withContext(Dispatchers.IO) {
        waypointDao.deleteWaypointById(id)
        try {
            firestore.collection("waypoints").document(id.toString()).delete()
        } catch (_: Exception) {}
    }

    // --- ROBOTS ---
    suspend fun insertRobot(robot: RobotEntity): Long = withContext(Dispatchers.IO) {
        val localId = robotDao.insertRobot(robot)

        val robotData = mapOf(
            "id" to localId,
            "robotName" to robot.robotName,
            "robotNumber" to robot.robotNumber,
            "status" to robot.status,
            "batteryLevel" to robot.batteryLevel,
            "zoneId" to robot.zoneId
        )
        try {
            firestore.collection("robots").document(localId.toString()).set(robotData)
        } catch (_: Exception) {}

        localId
    }

    val allRobots: Flow<List<RobotEntity>> = robotDao.observeAllRobots()
    val robotsWithZone: Flow<List<RobotWithZone>> = robotDao.observeRobotsWithZone()

    suspend fun deleteRobot(id: Long) = withContext(Dispatchers.IO) {
        robotDao.deleteRobotById(id)
        try {
            firestore.collection("robots").document(id.toString()).delete()
        } catch (_: Exception) {}
    }

    // --- TASKS ---
    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        var targetRobotId = task.robotId
        var targetWaypointId = task.waypointId

        if (robotDao.getRobotById(targetRobotId) == null) {
            targetRobotId = robotDao.insertRobot(
                RobotEntity(robotName = "Default Robot", robotNumber = "R-001", status = "Idle", batteryLevel = 100, zoneId = null)
            )
        }

        if (waypointDao.getWaypointById(targetWaypointId) == null) {
            val defaultZoneId = zoneDao.insertZone(ZoneEntity(name = "Default Zone", description = "Auto-created zone"))
            targetWaypointId = waypointDao.insertWaypoint(
                WaypointEntity(name = "Default Waypoint", xCord = 0.0, yCord = 0.0, zoneId = defaultZoneId)
            )
        }

        val adjustedTask = task.copy(robotId = targetRobotId, waypointId = targetWaypointId)
        val localId = taskDao.insertTask(adjustedTask)

        val taskData = mapOf(
            "id" to localId,
            "robotId" to targetRobotId,
            "waypointId" to targetWaypointId,
            "status" to adjustedTask.status,
            "priority" to adjustedTask.priority,
            "createdAt" to adjustedTask.createdAt
        )
        try {
            firestore.collection("tasks").document(localId.toString()).set(taskData)
        } catch (_: Exception) {}

        localId
    }

    val allTasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    val tasksWithDetails: Flow<List<TaskWithDetails>> = taskDao.observeTasksWithDetails()
    fun getTasksByRobot(robotId: Long): Flow<List<TaskEntity>> = taskDao.observeTasksByRobot(robotId)

    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(id)
        try {
            firestore.collection("tasks").document(id.toString()).delete()
        } catch (_: Exception) {}
    }
}
