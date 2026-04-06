package com.jqwave.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [EventConfigEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventConfigDao(): EventConfigDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val json = DefaultEventRules.shabbatRules.toJson().replace("'", "''")
                db.execSQL(
                    "INSERT OR IGNORE INTO event_configs (kind, enabled, rulesJson) VALUES ('SHABBAT', 0, '$json');",
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "jqwave.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
