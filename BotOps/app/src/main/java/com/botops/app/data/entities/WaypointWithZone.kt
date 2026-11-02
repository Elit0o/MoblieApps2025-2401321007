package com.botops.app.data.entities

import androidx.room.Embedded

data class WaypointWithZone(
    @Embedded val waypoint: WaypointEntity,
    val zoneName: String?
)
