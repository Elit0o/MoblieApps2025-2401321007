package com.botops.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.botops.app.data.dao.RobotDao
import com.botops.app.data.dao.TaskDao
import com.botops.app.data.dao.WaypointDao
import com.botops.app.data.dao.ZoneDao
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.ZoneEntity

@Database(
    entities = [
        ZoneEntity::class,
        WaypointEntity::class,
        RobotEntity::class,
        TaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BotOpsDatabase : RoomDatabase() {

    abstract fun zoneDao(): ZoneDao
    abstract fun waypointDao(): WaypointDao
    abstract fun robotDao(): RobotDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: BotOpsDatabase? = null

        fun getInstance(context: Context): BotOpsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BotOpsDatabase::class.java,
                    "botops_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}