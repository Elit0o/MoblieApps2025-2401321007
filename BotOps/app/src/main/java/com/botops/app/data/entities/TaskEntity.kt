package com.botops.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["robotId"]),
        Index(value = ["waypointId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = RobotEntity::class,
            parentColumns = ["id"],
            childColumns = ["robotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WaypointEntity::class,
            parentColumns = ["id"],
            childColumns = ["waypointId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val robotId: Long,
    val waypointId: Long,
    val status: String,
    val priority: Int,
    val createdAt: Long
)