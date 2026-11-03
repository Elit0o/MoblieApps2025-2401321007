package com.botops.app.data.entities

import androidx.room.Embedded

data class TaskWithDetails(
    @Embedded val task: TaskEntity,
    val robotName: String?,
    val robotBattery: Int?,
    val robotStatus: String?,
    val waypointName: String?,
    val zoneName: String?
)
