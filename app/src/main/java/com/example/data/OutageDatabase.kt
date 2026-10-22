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

@Entity(tableName = "cached_snapshots")
data class CachedSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val rawJson: String,
    val savedAtMillis: Long
)

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM cached_snapshots WHERE id = 1 LIMIT 1")
    fun observeCachedSnapshot(): Flow<CachedSnapshotEntity?>

    @Query("SELECT * FROM cached_snapshots WHERE id = 1 LIMIT 1")
    suspend fun getCachedSnapshot(): CachedSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(entity: CachedSnapshotEntity)
}

@Database(entities = [CachedSnapshotEntity::class], version = 1, exportSchema = false)
abstract class OutageDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
}
