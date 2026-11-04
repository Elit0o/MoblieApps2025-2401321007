package com.botops.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["zoneId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["zoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)

data class WaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val xCord: Double,
    val yCord: Double,
    val zoneId: Long // Външен ключ към ZoneEntity
)