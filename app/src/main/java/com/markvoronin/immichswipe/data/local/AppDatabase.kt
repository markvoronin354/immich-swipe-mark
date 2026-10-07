package com.markvoronin.immichswipe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.dao.SwipeDecisionDao
import com.markvoronin.immichswipe.data.local.dao.UserAccountDao
import com.markvoronin.immichswipe.data.local.entity.AlbumAssetEntity
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity


@Database(
    entities = [SwipeDecisionEntity::class, SyncHistoryEntity::class, AlbumAssetEntity::class, UserAccountEntity::class],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun swipeDecisionDao(): SwipeDecisionDao
    abstract fun albumAssetDao(): AlbumAssetDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 12 -> 13 (Adding composite indexes for sorted queries)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_assets_albumId_userId_fileCreatedAt ON album_assets (albumId, userId, fileCreatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_assets_albumId_userId_fileSizeInBytes ON album_assets (albumId, userId, fileSizeInBytes)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_assets_albumId_userId_type_fileCreatedAt ON album_assets (albumId, userId, type, fileCreatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_swipe_decisions_userId_albumId ON swipe_decisions (userId, albumId)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 11 -> 12 (Adding rotation to album_assets)")
                db.execSQL("ALTER TABLE album_assets ADD COLUMN rotation INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 10 -> 11 (Adding metadata to album_assets)")
                db.execSQL("ALTER TABLE album_assets ADD COLUMN type TEXT")
                db.execSQL("ALTER TABLE album_assets ADD COLUMN fileCreatedAt TEXT")
                db.execSQL("ALTER TABLE album_assets ADD COLUMN originalFileName TEXT")
                db.execSQL("ALTER TABLE album_assets ADD COLUMN fileSizeInBytes INTEGER")
                db.execSQL("ALTER TABLE album_assets ADD COLUMN imageWidth INTEGER")
                db.execSQL("ALTER TABLE album_assets ADD COLUMN imageHeight INTEGER")
            }
        }


        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 9 -> 10 (Modifying album_assets)")
                db.execSQL("DROP TABLE IF EXISTS album_assets")
                
                db.execSQL("""
                    CREATE TABLE album_assets (
                        albumId TEXT NOT NULL,
                        assetId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        PRIMARY KEY(albumId, assetId, userId)
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_assets_assetId ON album_assets (assetId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_assets_userId ON album_assets (userId)")
            }
        }


        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 8 -> 9 (Adding user_accounts)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_accounts (
                        userId TEXT NOT NULL,
                        baseUrl TEXT NOT NULL,
                        apiKey TEXT NOT NULL,
                        userName TEXT,
                        userEmail TEXT NOT NULL,
                        avatarColor TEXT,
                        lastActive INTEGER NOT NULL,
                        PRIMARY KEY(userId)
                    )
                """.trimIndent())
            }
        }


        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 7 -> 8 (Adding wasSyncedSkip)")
                db.execSQL("ALTER TABLE swipe_decisions ADD COLUMN wasSyncedSkip INTEGER NOT NULL DEFAULT 0")
            }
        }


        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 6 -> 7 (Modifying PK swipe_decisions)")
                db.execSQL("""
                    CREATE TABLE swipe_decisions_new (
                        assetId TEXT NOT NULL,
                        albumId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        decision TEXT NOT NULL,
                        fileSize INTEGER,
                        createdAt INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        PRIMARY KEY(assetId, userId)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO swipe_decisions_new (assetId, albumId, userId, decision, fileSize, createdAt, isSynced)
                    SELECT assetId, albumId, userId, decision, fileSize, createdAt, isSynced
                    FROM swipe_decisions
                    GROUP BY assetId, userId
                """.trimIndent())

                db.execSQL("DROP TABLE swipe_decisions")
                db.execSQL("ALTER TABLE swipe_decisions_new RENAME TO swipe_decisions")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 5 -> 6 (Adding album_assets)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS album_assets (
                        albumId TEXT NOT NULL,
                        assetId TEXT NOT NULL,
                        PRIMARY KEY(albumId, assetId)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_assets_assetId ON album_assets (assetId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 2 -> 3 (Adding fileSize)")
                db.execSQL("ALTER TABLE swipe_decisions ADD COLUMN fileSize INTEGER DEFAULT NULL")
            }
        }


        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 3 -> 4 (Adding userId PK)")
                db.execSQL("""
                    CREATE TABLE swipe_decisions_new (
                        assetId TEXT NOT NULL,
                        albumId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        decision TEXT NOT NULL,
                        fileSize INTEGER,
                        createdAt INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        PRIMARY KEY(assetId, albumId, userId)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO swipe_decisions_new (assetId, albumId, userId, decision, fileSize, createdAt, isSynced)
                    SELECT assetId, albumId, 'legacy_user', decision, fileSize, createdAt, isSynced
                    FROM swipe_decisions
                """.trimIndent())

                db.execSQL("DROP TABLE swipe_decisions")
                db.execSQL("ALTER TABLE swipe_decisions_new RENAME TO swipe_decisions")
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("Database", "Executing Migration 4 -> 5 (Adding sync_history)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        deletedCount INTEGER NOT NULL,
                        bytesSaved INTEGER NOT NULL,
                        keptCount INTEGER NOT NULL,
                        archivedCount INTEGER NOT NULL,
                        lockedCount INTEGER NOT NULL,
                        skippedCount INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "immich_swipe_database"
                            )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
