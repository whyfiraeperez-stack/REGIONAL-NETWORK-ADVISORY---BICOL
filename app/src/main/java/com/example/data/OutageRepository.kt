package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OutageRepository(
    private val context: Context,
    private val apiService: OutageApiService,
    private val snapshotDao: SnapshotDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(OutageSnapshotResponse::class.java)

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
                // Save to local Room cache
                val rawJson = adapter.toJson(response)
                snapshotDao.saveSnapshot(
                    CachedSnapshotEntity(
                        id = 1,
                        rawJson = rawJson,
                        savedAtMillis = System.currentTimeMillis()
                    )
                )
                return@withContext SnapshotLoadResult(
                    snapshot = response,
                    isLiveFromNetwork = true,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            // Fall through to Room cache or bundled initial snapshot
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
            val cached = snapshotDao.getCachedSnapshot()
            if (cached != null && cached.rawJson.isNotBlank()) {
                val parsed = adapter.fromJson(cached.rawJson)
                if (parsed != null) return@withContext parsed
            }
        } catch (_: Exception) {
        }

        // Load bundled seed snapshot captured from live API
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
