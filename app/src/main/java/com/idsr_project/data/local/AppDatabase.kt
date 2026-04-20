package com.idsr_project.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PendingReportEntity::class,
        RegionEntity::class,
        DistrictEntity::class,
        FacilityEntity::class],

    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingReportDao(): PendingReportDao
    abstract fun referenceDataDao(): ReferenceDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_reports ADD COLUMN regionName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pending_reports ADD COLUMN districtName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS regions (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS districts (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        regionId INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS facilities (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        districtId INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "idsr_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}