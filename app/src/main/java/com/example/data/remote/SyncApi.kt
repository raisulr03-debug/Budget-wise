package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

// Encrypted transaction details model for network transfer
data class CloudSyncPayload(
    val deviceId: String,
    val lastSyncTimestamp: Long,
    val encryptedTransactions: String // AES-GCM base64 string
)

// Response from sync server
data class CloudSyncResponse(
    val success: Boolean,
    val message: String,
    val serverTimestamp: Long,
    val encryptedTransactions: String? // Decryptable transaction list updates
)

interface SyncApi {
    @POST
    suspend fun syncData(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body payload: CloudSyncPayload
    ): Response<CloudSyncResponse>
}
