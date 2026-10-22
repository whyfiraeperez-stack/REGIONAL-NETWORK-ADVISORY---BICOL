package com.example.data

import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface OutageApiService {
    @GET("api/snapshot")
    suspend fun fetchLiveSnapshot(
        @Query("refresh") refresh: Int = 1,
        @Query("ts") timestamp: Long = System.currentTimeMillis()
    ): OutageSnapshotResponse

    companion object {
        const val BASE_URL = "https://almasor-outage-monitoring.vercel.app/"

        fun create(): OutageApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OutageApiService::class.java)
        }
    }
}
