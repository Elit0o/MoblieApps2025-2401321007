package com.botops.app.data.repository

import android.content.Context
import android.util.Log
import com.botops.app.data.dao.RobotDao
import com.botops.app.data.dao.TaskDao
import com.botops.app.data.dao.WaypointDao
import com.botops.app.data.dao.ZoneDao
import com.botops.app.data.entities.*
import com.botops.app.data.network.RobotSocketClient
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BotOpsRepository(
    private val zoneDao: ZoneDao,
    private val waypointDao: WaypointDao,
    private val robotDao: RobotDao,
    private val taskDao: TaskDao,
    private val context: Context? = null,
    private val repositoryScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to get FirebaseFirestore instance", e)
            null
        }
    }
    private val socketClient = RobotSocketClient(context)
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    companion object {
        const val MIN_BATTERY_LEVEL = 20
        private const val TAG = "BotOpsRepository"
    }

    init {
        startRealtimeSync()
        startFleetSimulator()
    }

    private fun isDeletedLocally(entityType: String, id: Long): Boolean {
        val prefs = context?.getSharedPreferences("deleted_prefs", Context.MODE_PRIVATE) ?: return false
        val deletedSet = prefs.getStringSet(entityType, emptySet()) ?: emptySet()
        return deletedSet.contains(id.toString())
    }

    private fun markAsDeletedLocally(entityType: String, id: Long) {
        val prefs = context?.getSharedPreferences("deleted_prefs", Context.MODE_PRIVATE) ?: return
        val deletedSet = prefs.getStringSet(entityType, emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedSet.add(id.toString())
        prefs.edit().putStringSet(entityType, deletedSet).apply()
    }

    /**
     * Start real-time sync listeners for Firestore remote collections safely.
     */
    fun startRealtimeSync() {
        val fs = firestore ?: run {
            Log.w(TAG, "Firestore is unavailable. Operating strictly offline in Room.")
            return
        }

        // --- ZONES LISTENER ---
        try {
            val zoneListener = fs.collection("zones")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.documentChanges?.forEach { change ->
                        val doc = change.document
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@forEach
                        if (isDeletedLocally("zones", id)) return@forEach

                        val name = doc.getString("name") ?: ""
                        val description = doc.getString("description") ?: ""

                        repositoryScope.launch(Dispatchers.IO) {
                            try {
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        zoneDao.insertZone(ZoneEntity(id = id, name = name, description = description))
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        zoneDao.deleteZoneById(id)
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }
            listenerRegistrations.add(zoneListener)
        } catch (_: Throwable) {}

        // --- WAYPOINTS LISTENER ---
        try {
            val waypointListener = fs.collection("waypoints")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.documentChanges?.forEach { change ->
                        val doc = change.document
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@forEach
                        if (isDeletedLocally("waypoints", id)) return@forEach

                        val name = doc.getString("name") ?: ""
                        val xCord = doc.getDouble("xCord") ?: 0.0
                        val yCord = doc.getDouble("yCord") ?: 0.0
                        val zoneId = doc.getLong("zoneId") ?: 0L

                        repositoryScope.launch(Dispatchers.IO) {
                            try {
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        waypointDao.insertWaypoint(WaypointEntity(id = id, name = name, xCord = xCord, yCord = yCord, zoneId = zoneId))
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        waypointDao.deleteWaypointById(id)
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }
            listenerRegistrations.add(waypointListener)
        } catch (_: Throwable) {}

        // --- ROBOTS LISTENER ---
        try {
            val robotListener = fs.collection("robots")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.documentChanges?.forEach { change ->
                        val doc = change.document
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@forEach
                        if (isDeletedLocally("robots", id)) return@forEach

                        val name = doc.getString("robotName") ?: ""
                        val number = doc.getString("robotNumber") ?: ""
                        val status = doc.getString("status") ?: "Idle"
                        val battery = doc.getLong("batteryLevel")?.toInt() ?: 100
                        val zoneId = doc.getLong("zoneId")

                        repositoryScope.launch(Dispatchers.IO) {
                            try {
                                val existingRobot = robotDao.getRobotById(id)
                                val finalBattery = if (existingRobot != null && existingRobot.batteryLevel > battery) existingRobot.batteryLevel else battery
                                val finalStatus = if (existingRobot != null && existingRobot.status == "Charging" && finalBattery < 100) "Charging" else status

                                val targetZoneId = if (zoneId != null && zoneDao.getZoneById(zoneId) != null) zoneId else null
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        robotDao.insertRobot(RobotEntity(id = id, robotName = name, robotNumber = number, status = finalStatus, batteryLevel = finalBattery, zoneId = targetZoneId))
                                        evaluateTaskStatusesForRobot(id)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        robotDao.deleteRobotById(id)
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }
            listenerRegistrations.add(robotListener)
        } catch (_: Throwable) {}

        // --- TASKS LISTENER ---
        try {
            val taskListener = fs.collection("tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.documentChanges?.forEach { change ->
                        val doc = change.document
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@forEach
                        if (isDeletedLocally("tasks", id)) return@forEach

                        val title = doc.getString("title") ?: "Inspection Task"
                        val robotId = doc.getLong("robotId") ?: 0L
                        val waypointId = doc.getLong("waypointId") ?: 0L
                        val status = doc.getString("status") ?: "Pending"
                        val priority = doc.getLong("priority")?.toInt() ?: 1
                        val progress = doc.getLong("progress")?.toInt() ?: 0
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        repositoryScope.launch(Dispatchers.IO) {
                            try {
                                val existingTask = taskDao.getTaskById(id)
                                val finalProgress = if (existingTask != null && existingTask.progress > progress) existingTask.progress else progress
                                val finalStatus = if (existingTask != null && existingTask.status == "Completed") "Completed" else status

                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        taskDao.insertTask(TaskEntity(id = id, title = title, robotId = robotId, waypointId = waypointId, status = finalStatus, priority = priority, progress = finalProgress, createdAt = createdAt))
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        taskDao.deleteTaskById(id)
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }
            listenerRegistrations.add(taskListener)
        } catch (_: Throwable) {}
    }

    /**
     * Background Simulator Engine ("Making it Alive"):
     * Updates both local Room DB and syncs progress/battery changes to Firestore in real-time.
     */
    private fun startFleetSimulator() {
        repositoryScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    delay(5000) // Every 5 seconds for smooth, gradual, realistic progression

                    // 1. Process Robot Charging (+2% per tick)
                    val allRobots = robotDao.getAllRobotsSync()
                    for (robot in allRobots) {
                        if (robot.status.equals("Charging", true)) {
                            val newBattery = (robot.batteryLevel + 2).coerceAtMost(100)
                            val newStatus = if (newBattery >= 100) "Idle" else "Charging"
                            val updatedRobot = robot.copy(batteryLevel = newBattery, status = newStatus)
                            robotDao.updateRobot(updatedRobot)
                            try {
                                firestore?.collection("robots")?.document(robot.id.toString())?.update(mapOf("batteryLevel" to newBattery, "status" to newStatus))
                            } catch (_: Throwable) {}
                            evaluateTaskStatusesForRobot(robot.id)
                        } else if (robot.batteryLevel < MIN_BATTERY_LEVEL && robot.status != "Maintenance" && robot.status != "Charging") {
                            val updatedRobot = robot.copy(status = "Charging")
                            robotDao.updateRobot(updatedRobot)
                            try {
                                firestore?.collection("robots")?.document(robot.id.toString())?.update("status", "Charging")
                            } catch (_: Throwable) {}
                        }
                    }

                    // 2. Process Active Tasks Execution Progress (+2% per tick)
                    val allTasks = taskDao.getAllTasksSync()
                    for (task in allTasks) {
                        if (task.status.equals("Active", true)) {
                            val nextProgress = (task.progress + 2).coerceAtMost(100)
                            if (nextProgress >= 100) {
                                val completedTask = task.copy(progress = 100, status = "Completed")
                                taskDao.insertTask(completedTask)
                                try {
                                    firestore?.collection("tasks")?.document(task.id.toString())?.update(mapOf("status" to "Completed", "progress" to 100))
                                } catch (_: Throwable) {}

                                val robot = robotDao.getRobotById(task.robotId)
                                if (robot != null) {
                                    val drainedBattery = (robot.batteryLevel - 10).coerceAtLeast(0)
                                    val robotStatus = if (drainedBattery < MIN_BATTERY_LEVEL) "Charging" else robot.status
                                    robotDao.updateRobot(robot.copy(batteryLevel = drainedBattery, status = robotStatus))
                                    try {
                                        firestore?.collection("robots")?.document(robot.id.toString())?.update(mapOf("batteryLevel" to drainedBattery, "status" to robotStatus))
                                    } catch (_: Throwable) {}
                                    evaluateTaskStatusesForRobot(robot.id)
                                }
                            } else {
                                val progressingTask = task.copy(progress = nextProgress)
                                taskDao.insertTask(progressingTask)
                                try {
                                    firestore?.collection("tasks")?.document(task.id.toString())?.update("progress", nextProgress)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error in simulator tick", e)
                }
            }
        }
    }

    fun stopRealtimeSync() {
        try {
            listenerRegistrations.forEach { it.remove() }
            listenerRegistrations.clear()
        } catch (_: Throwable) {}
    }

    // --- ZONES ---
    suspend fun insertZone(zone: ZoneEntity): Long = withContext(Dispatchers.IO) {
        val localId = zoneDao.insertZone(zone)
        val zoneData = mapOf("id" to localId, "name" to zone.name, "description" to zone.description)
        try { firestore?.collection("zones")?.document(localId.toString())?.set(zoneData) } catch (_: Throwable) {}
        localId
    }

    suspend fun updateZone(zone: ZoneEntity) = withContext(Dispatchers.IO) {
        zoneDao.updateZone(zone)
        val zoneData = mapOf("id" to zone.id, "name" to zone.name, "description" to zone.description)
        try { firestore?.collection("zones")?.document(zone.id.toString())?.set(zoneData) } catch (_: Throwable) {}
    }

    val allZones: Flow<List<ZoneEntity>> = zoneDao.observeAllZones()

    suspend fun deleteZone(id: Long) = withContext(Dispatchers.IO) {
        markAsDeletedLocally("zones", id)
        zoneDao.deleteZoneById(id)
        try { firestore?.collection("zones")?.document(id.toString())?.delete() } catch (_: Throwable) {}
    }

    // --- WAYPOINTS ---
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long = withContext(Dispatchers.IO) {
        val localId = waypointDao.insertWaypoint(waypoint)
        val waypointData = mapOf("id" to localId, "name" to waypoint.name, "xCord" to waypoint.xCord, "yCord" to waypoint.yCord, "zoneId" to waypoint.zoneId)
        try { firestore?.collection("waypoints")?.document(localId.toString())?.set(waypointData) } catch (_: Throwable) {}
        localId
    }

    suspend fun updateWaypoint(waypoint: WaypointEntity) = withContext(Dispatchers.IO) {
        waypointDao.updateWaypoint(waypoint)
        val waypointData = mapOf("id" to waypoint.id, "name" to waypoint.name, "xCord" to waypoint.xCord, "yCord" to waypoint.yCord, "zoneId" to waypoint.zoneId)
        try { firestore?.collection("waypoints")?.document(waypoint.id.toString())?.set(waypointData) } catch (_: Throwable) {}
    }

    val allWaypoints: Flow<List<WaypointEntity>> = waypointDao.observeAllWaypoints()
    val waypointsWithZone: Flow<List<WaypointWithZone>> = waypointDao.observeWaypointsWithZone()
    fun getWaypointsByZone(zoneId: Long): Flow<List<WaypointEntity>> = waypointDao.observeWaypointsByZone(zoneId)

    suspend fun deleteWaypoint(id: Long) = withContext(Dispatchers.IO) {
        markAsDeletedLocally("waypoints", id)
        waypointDao.deleteWaypointById(id)
        try { firestore?.collection("waypoints")?.document(id.toString())?.delete() } catch (_: Throwable) {}
    }

    // --- ROBOTS ---
    suspend fun insertRobot(robot: RobotEntity): Long = withContext(Dispatchers.IO) {
        val localId = robotDao.insertRobot(robot)
        val robotData = mapOf("id" to localId, "robotName" to robot.robotName, "robotNumber" to robot.robotNumber, "status" to robot.status, "batteryLevel" to robot.batteryLevel, "zoneId" to robot.zoneId)
        try { firestore?.collection("robots")?.document(localId.toString())?.set(robotData) } catch (_: Throwable) {}

        repositoryScope.launch(Dispatchers.IO) {
            socketClient.sendCommand("REGISTER_ROBOT:id=$localId,name=${robot.robotName},status=${robot.status},battery=${robot.batteryLevel}")
        }

        evaluateTaskStatusesForRobot(localId)
        localId
    }

    suspend fun updateRobot(robot: RobotEntity) = withContext(Dispatchers.IO) {
        robotDao.updateRobot(robot)
        val robotData = mapOf("id" to robot.id, "robotName" to robot.robotName, "robotNumber" to robot.robotNumber, "status" to robot.status, "batteryLevel" to robot.batteryLevel, "zoneId" to robot.zoneId)
        try { firestore?.collection("robots")?.document(robot.id.toString())?.set(robotData) } catch (_: Throwable) {}

        repositoryScope.launch(Dispatchers.IO) {
            socketClient.sendCommand("UPDATE_ROBOT:id=${robot.id},name=${robot.robotName},status=${robot.status},battery=${robot.batteryLevel}")
        }

        evaluateTaskStatusesForRobot(robot.id)
    }

    val allRobots: Flow<List<RobotEntity>> = robotDao.observeAllRobots()
    val robotsWithZone: Flow<List<RobotWithZone>> = robotDao.observeRobotsWithZone()

    suspend fun deleteRobot(id: Long) = withContext(Dispatchers.IO) {
        markAsDeletedLocally("robots", id)
        robotDao.deleteRobotById(id)
        try { firestore?.collection("robots")?.document(id.toString())?.delete() } catch (_: Throwable) {}

        repositoryScope.launch(Dispatchers.IO) {
            socketClient.sendCommand("DELETE_ROBOT:id=$id")
        }
    }

    // --- TASKS ---
    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        val localId = taskDao.insertTask(task)

        evaluateTaskStatusesForRobot(task.robotId)

        val updatedTask = taskDao.getTaskById(localId) ?: task

        val taskData = mapOf(
            "id" to localId,
            "title" to updatedTask.title,
            "robotId" to updatedTask.robotId,
            "waypointId" to updatedTask.waypointId,
            "status" to updatedTask.status,
            "priority" to updatedTask.priority,
            "progress" to updatedTask.progress,
            "createdAt" to updatedTask.createdAt
        )
        try { firestore?.collection("tasks")?.document(localId.toString())?.set(taskData) } catch (_: Throwable) {}

        repositoryScope.launch(Dispatchers.IO) {
            socketClient.sendCommand("ASSIGN_TASK:taskId=$localId,title=${updatedTask.title},robotId=${updatedTask.robotId},waypointId=${updatedTask.waypointId},priority=${updatedTask.priority},status=${updatedTask.status}")
        }

        localId
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
        evaluateTaskStatusesForRobot(task.robotId)
        val updatedTask = taskDao.getTaskById(task.id) ?: task
        val taskData = mapOf(
            "id" to updatedTask.id,
            "title" to updatedTask.title,
            "robotId" to updatedTask.robotId,
            "waypointId" to updatedTask.waypointId,
            "status" to updatedTask.status,
            "priority" to updatedTask.priority,
            "progress" to updatedTask.progress,
            "createdAt" to updatedTask.createdAt
        )
        try { firestore?.collection("tasks")?.document(updatedTask.id.toString())?.set(taskData) } catch (_: Throwable) {}

        repositoryScope.launch(Dispatchers.IO) {
            socketClient.sendCommand("UPDATE_TASK:taskId=${updatedTask.id},title=${updatedTask.title},robotId=${updatedTask.robotId},waypointId=${updatedTask.waypointId},status=${updatedTask.status},priority=${updatedTask.priority}")
        }
    }

    suspend fun markTaskAsCompleted(taskId: Long) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext
        val updatedTask = task.copy(status = "Completed", progress = 100)
        taskDao.insertTask(updatedTask)
        try { firestore?.collection("tasks")?.document(taskId.toString())?.update(mapOf("status" to "Completed", "progress" to 100)) } catch (_: Throwable) {}

        repositoryScope.launch(Dispatchers.IO) {
            socketClient.sendCommand("COMPLETE_TASK:taskId=$taskId,robotId=${task.robotId}")
        }

        evaluateTaskStatusesForRobot(task.robotId)
    }

    suspend fun pauseTask(taskId: Long) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext
        val newStatus = if (task.status.equals("Paused", true)) "Active" else "Paused"
        val updatedTask = task.copy(status = newStatus)
        taskDao.insertTask(updatedTask)
        try {
            firestore?.collection("tasks")?.document(taskId.toString())?.update("status", newStatus)
        } catch (_: Throwable) {}
        evaluateTaskStatusesForRobot(task.robotId)
    }

    suspend fun cancelTask(taskId: Long) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext
        val updatedTask = task.copy(status = "Cancelled")
        taskDao.insertTask(updatedTask)
        try {
            firestore?.collection("tasks")?.document(taskId.toString())?.update("status", "Cancelled")
        } catch (_: Throwable) {}
        evaluateTaskStatusesForRobot(task.robotId)
    }

    val allTasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    val tasksWithDetails: Flow<List<TaskWithDetails>> = taskDao.observeTasksWithDetails()
    fun getTasksByRobot(robotId: Long): Flow<List<TaskEntity>> = taskDao.observeTasksByRobot(robotId)

    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) {
        markAsDeletedLocally("tasks", id)
        val task = taskDao.getTaskById(id)
        val robotId = task?.robotId
        taskDao.deleteTaskById(id)
        try { firestore?.collection("tasks")?.document(id.toString())?.delete() } catch (_: Throwable) {}

        repositoryScope.launch(Dispatchers.IO) {
            socketClient.sendCommand("DELETE_TASK:taskId=$id")
        }

        robotId?.let { evaluateTaskStatusesForRobot(it) }
    }

    suspend fun evaluateTaskStatusesForRobot(robotId: Long) = withContext(Dispatchers.IO) {
        try {
            val robot = robotDao.getRobotById(robotId) ?: return@withContext
            val uncompletedTasks = taskDao.getActiveAndPendingTasksForRobotSync(robotId)

            if (uncompletedTasks.isEmpty()) return@withContext

            val hasEnoughBattery = robot.batteryLevel >= MIN_BATTERY_LEVEL && robot.status != "Maintenance" && robot.status != "Charging"

            uncompletedTasks.forEachIndexed { index, task ->
                val expectedStatus = if (index == 0 && hasEnoughBattery) "Active" else "Pending"
                if (task.status != expectedStatus) {
                    val updatedStatusTask = task.copy(status = expectedStatus)
                    taskDao.insertTask(updatedStatusTask)

                    try {
                        firestore?.collection("tasks")?.document(task.id.toString())?.update("status", expectedStatus)
                    } catch (_: Throwable) {}

                    repositoryScope.launch(Dispatchers.IO) {
                        socketClient.sendCommand("TASK_STATUS_CHANGED:taskId=${task.id},status=$expectedStatus,robotId=$robotId")
                    }
                }
            }
        } catch (_: Throwable) {}
    }
}
