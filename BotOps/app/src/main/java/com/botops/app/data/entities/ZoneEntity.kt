package com.botops.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "zones",
    indices = [Index(value = ["name"], unique = true)]
)
data class ZoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val minX: Double = 0.0,
    val maxX: Double = 100.0,
    val minY: Double = 0.0,
    val maxY: Double = 100.0
)
