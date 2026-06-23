package com.photocleaner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.photocleaner.core.database.converter.ListTypeConverter
import com.photocleaner.core.database.dao.DuplicateGroupDao
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.dao.RecycleDao
import com.photocleaner.core.database.dao.ScanCheckpointDao
import com.photocleaner.core.database.entity.DuplicateGroupEntity
import com.photocleaner.core.database.entity.GroupMemberEntity
import com.photocleaner.core.database.entity.ImageItemEntity
import com.photocleaner.core.database.entity.RecycleItemEntity
import com.photocleaner.core.database.entity.ScanCheckpointEntity

/**
 * Room database for the PhotoCleaner core database module.
 *
 * Exposes three DAOs for image scanning, duplicate-group management, and
 * recycle-bin operations. The database uses Room's built-in [RoomDatabase]
 * infrastructure with a singleton pattern to avoid multiple concurrent instances.
 *
 * Entities registered:
 * - [ImageItemEntity]         — scanned image metadata
 * - [DuplicateGroupEntity]    — duplicate/similar-image groups
 * - [GroupMemberEntity]       — many-to-many join between groups and images
 * - [RecycleItemEntity]       — soft-deleted images in the recycle area
 * - [ScanCheckpointEntity]    — scan progress checkpoint for resume
 */
@Database(
    entities = [
        ImageItemEntity::class,
        DuplicateGroupEntity::class,
        GroupMemberEntity::class,
        RecycleItemEntity::class,
        ScanCheckpointEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun imageDao(): ImageDao
    abstract fun duplicateGroupDao(): DuplicateGroupDao
    abstract fun recycleDao(): RecycleDao
    abstract fun scanCheckpointDao(): ScanCheckpointDao

    companion object {
        /**
         * Database file name used by Room.
         */
        private const val DATABASE_NAME = "photocleaner.db"

        /**
         * Volatile reference to the singleton instance for thread-safe double-checked locking.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton [AppDatabase], creating it if necessary.
         *
         * @param context Application context (used only once during initial creation).
         * @return The shared [AppDatabase] instance.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Builds a new [AppDatabase] instance using the builder pattern.
         *
         * @param context Application context.
         * @return A newly constructed [AppDatabase].
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addTypeConverter(ListTypeConverter())
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
