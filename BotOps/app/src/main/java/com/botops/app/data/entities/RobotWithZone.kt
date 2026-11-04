package com.botops.app.data.entities

import androidx.room.Embedded

data class RobotWithZone(
    @Embedded val robot: RobotEntity,
    val zoneName: String?
)
