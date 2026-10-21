package com.botops.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.ZoneEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BotOpsViewModel(private val repository: BotOpsRepository) : ViewModel() {

    val zones: StateFlow<List<ZoneEntity>> = repository.allZones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waypoints: StateFlow<List<WaypointEntity>> = repository.allWaypoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val robots: StateFlow<List<RobotEntity>> = repository.allRobots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertZone(name: String, description: String) {
        viewModelScope.launch {
            repository.insertZone(ZoneEntity(name = name, description = description))
        }
    }

    fun deleteZone(id: Long) {
        viewModelScope.launch {
            repository.deleteZone(id)
        }
    }

    fun insertWaypoint(name: String, xCoord: Double, yCoord: Double, zoneId: Long) {
        viewModelScope.launch {
            repository.insertWaypoint(WaypointEntity(name = name, xCord = xCoord, yCord = yCoord, zoneId = zoneId))
        }
    }

    fun deleteWaypoint(id: Long) {
        viewModelScope.launch {
            repository.deleteWaypoint(id)
        }
    }

    fun insertRobot(name: String, number: String, status: String, battery: Int, zoneId: Long?) {
        viewModelScope.launch {
            repository.insertRobot(
                RobotEntity(robotName = name, robotNumber = number, status = status, batteryLevel = battery, zoneId = zoneId)
            )
        }
    }

    fun deleteRobot(id: Long) {
        viewModelScope.launch {
            repository.deleteRobot(id)
        }
    }

    fun insertTask(robotId: Long, waypointId: Long, status: String, priority: Int) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(robotId = robotId, waypointId = waypointId, status = status, priority = priority, createdAt = System.currentTimeMillis())
            )
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    class Factory(private val repository: BotOpsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BotOpsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BotOpsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}