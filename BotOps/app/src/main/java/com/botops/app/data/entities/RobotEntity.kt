package com.botops.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "robots",
    indices = [
        Index(value = ["robotName"], unique = true),
        Index(value = ["zoneId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["zoneId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class RobotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val robotName: String,
    val robotNumber: String,
    val status: String,
    val batteryLevel: Int,
    val zoneId: Long? = null 
)