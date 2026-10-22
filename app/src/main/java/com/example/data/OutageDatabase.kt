package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Stores metadata about the latest verified snapshot in SQLite (< 1 KB per row),
 * while the full 5 MB JSON payload is stored on disk in `filesDir/cached_snapshot.json`
 * to prevent SQLite CursorWindow (2 MB) overflow exceptions.
 */
@Entity(tableName = "cached_snapshot_meta")
data class CachedSnapshotMetaEntity(
    @PrimaryKey val id: Int = 1,
    val version: String,
    val syncState: String,
    val checkedAt: String,
    val pendingCount: Int,
    val restoredCount: Int,
    val totalRows: Int,
    val savedAtMillis: Long
)

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM cached_snapshot_meta WHERE id = 1 LIMIT 1")
    fun observeCachedMeta(): Flow<CachedSnapshotMetaEntity?>

    @Query("SELECT * FROM cached_snapshot_meta WHERE id = 1 LIMIT 1")
    suspend fun getCachedMeta(): CachedSnapshotMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMeta(entity: CachedSnapshotMetaEntity)
}

@Database(entities = [CachedSnapshotMetaEntity::class], version = 2, exportSchema = false)
abstract class OutageDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
}
