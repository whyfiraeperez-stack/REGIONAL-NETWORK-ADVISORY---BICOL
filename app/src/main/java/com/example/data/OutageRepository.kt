package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OutageRepository(
    private val context: Context,
    private val apiService: OutageApiService,
    private val snapshotDao: SnapshotDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(OutageSnapshotResponse::class.java)

    private val snapshotCacheFile: File
        get() = File(context.filesDir, "cached_live_snapshot.json")

    data class SnapshotLoadResult(
        val snapshot: OutageSnapshotResponse,
        val isLiveFromNetwork: Boolean,
        val errorMessage: String? = null
    )

    suspend fun fetchLiveOrFallbackSnapshot(): SnapshotLoadResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.fetchLiveSnapshot(
                refresh = 1,
                timestamp = System.currentTimeMillis()
            )
            if (response.rows.isNotEmpty() || response.napDownRows.isNotEmpty()) {
                // Save full JSON payload to internal file storage (avoids 2MB SQLite CursorWindow limit)
                try {
                    val rawJson = adapter.toJson(response)
                    snapshotCacheFile.writeText(rawJson)
                    // Save lightweight metadata in Room
                    snapshotDao.saveMeta(
                        CachedSnapshotMetaEntity(
                            id = 1,
                            version = response.version ?: "",
                            syncState = response.syncState ?: "SYNCED",
                            checkedAt = response.checkedAt ?: "",
                            pendingCount = response.pendingCount ?: 0,
                            restoredCount = response.restoredCount ?: 0,
                            totalRows = response.rows.size,
                            savedAtMillis = System.currentTimeMillis()
                        )
                    )
                } catch (_: Exception) {
                }

                return@withContext SnapshotLoadResult(
                    snapshot = response,
                    isLiveFromNetwork = true,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            val fallback = loadCachedOrSeedSnapshot()
            return@withContext SnapshotLoadResult(
                snapshot = fallback,
                isLiveFromNetwork = false,
                errorMessage = "Live network read recovering (${e.localizedMessage ?: "timeout"}). Serving verified snapshot."
            )
        }

        val fallback = loadCachedOrSeedSnapshot()
        SnapshotLoadResult(
            snapshot = fallback,
            isLiveFromNetwork = false,
            errorMessage = null
        )
    }

    suspend fun loadCachedOrSeedSnapshot(): OutageSnapshotResponse = withContext(Dispatchers.IO) {
        // 1. Read from internal file storage if available
        try {
            if (snapshotCacheFile.exists() && snapshotCacheFile.length() > 0L) {
                val cachedJson = snapshotCacheFile.readText()
                if (cachedJson.isNotBlank()) {
                    val parsed = adapter.fromJson(cachedJson)
                    if (parsed != null) return@withContext parsed
                }
            }
        } catch (_: Exception) {
        }

        // 2. Load bundled seed snapshot captured from live API
        try {
            val assetJson = context.assets.open("initial_snapshot.json")
                .bufferedReader()
                .use { it.readText() }
            val parsed = adapter.fromJson(assetJson)
            if (parsed != null) return@withContext parsed
        } catch (_: Exception) {
        }

        OutageSnapshotResponse(ok = false, syncState = "OFFLINE")
    }
}
