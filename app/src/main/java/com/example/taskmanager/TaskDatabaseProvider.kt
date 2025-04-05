package com.example.taskmanager

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase

object TaskDatabaseProvider {
    private var INSTANCE: TaskDatabase? = null

    fun getDatabase(context: Context): TaskDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                TaskDatabase::class.java,
                "task_database"
            )
                .addMigrations(
                    TaskDatabase.MIGRATION_1_2,
                    TaskDatabase.MIGRATION_2_3,
                    TaskDatabase.MIGRATION_3_4,
                    TaskDatabase.MIGRATION_4_5  // add the new migration
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate database on a background thread if needed.
                    }
                })
                .fallbackToDestructiveMigration() // Only as a last resort
                .build()

            INSTANCE = instance
            instance
        }
    }

    fun destroyInstance() {
        INSTANCE = null
    }
}

