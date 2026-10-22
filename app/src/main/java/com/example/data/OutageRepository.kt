package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OutageRepository(
    private val context: Context,
    private val apiService: OutageApiService,
    private val snapshotDao: SnapshotDao
) {
    // Uses KSP-generated adapters (@JsonClass(generateAdapter = true)) for zero-reflection speed
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(OutageSnapshotResponse::class.java)

    private val snapshotCacheFile: File
        get() = File(context.filesDir, "cached_compact_snapshot.json")

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
                // Save a compact snapshot (all PENDING + recent 400 rows + full statusCounts)
                // so file I/O is <150 KB and instantaneous on any phone
                try {
                    val pendingRows = response.rows.filter {
                        it.finalStatus?.trim()?.equals("PENDING", ignoreCase = true) == true
                    }
                    val otherRows = response.rows.filter {
                        it.finalStatus?.trim()?.equals("PENDING", ignoreCase = true) != true
                    }.takeLast(350)
                    val compactResponse = response.copy(rows = pendingRows + otherRows)
                    val rawJson = adapter.toJson(compactResponse)
                    snapshotCacheFile.writeText(rawJson)

                    snapshotDao.saveMeta(
                        CachedSnapshotMetaEntity(
                            id = 1,
                            version = response.version ?: "",
                            syncState = response.syncState ?: "SYNCED",
                            checkedAt = response.checkedAt ?: "",
                            pendingCount = response.pendingCount ?: pendingRows.size,
                            restoredCount = response.restoredCount ?: 0,
                            totalRows = response.diagnostics?.databaseRows ?: response.rows.size,
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
        try {
            if (snapshotCacheFile.exists() && snapshotCacheFile.length() in 1L..1_500_000L) {
                val cachedJson = snapshotCacheFile.readText()
                if (cachedJson.isNotBlank()) {
                    val parsed = adapter.fromJson(cachedJson)
                    if (parsed != null && (parsed.rows.isNotEmpty() || parsed.napDownRows.isNotEmpty())) {
                        return@withContext parsed
                    }
                }
            }
        } catch (_: Exception) {
        }

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
