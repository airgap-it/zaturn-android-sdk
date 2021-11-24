package ch.papers.zaturnsdk.internal.network

import ch.papers.zaturnsdk.internal.crypto.data.PublicKey
import ch.papers.zaturnsdk.internal.network.data.publickey.GetPublicKeyResponse
import ch.papers.zaturnsdk.internal.network.data.storage.CheckRecoveryPartResponse
import ch.papers.zaturnsdk.internal.network.data.storage.RetrieveRecoveryPartResponse
import ch.papers.zaturnsdk.internal.network.data.storage.StoreRecoveryPartRequest
import ch.papers.zaturnsdk.internal.network.data.storage.StoreRecoveryPartResponse
import ch.papers.zaturnsdk.internal.network.http.Http
import ch.papers.zaturnsdk.internal.network.http.data.Token
import ch.papers.zaturnsdk.internal.util.async
import ch.papers.zaturnsdk.internal.util.filterValuesNotNull
import ch.papers.zaturnsdk.internal.util.launchIndexed

internal class ZaturnNode(val id: String, private val http: Http) {
    suspend fun publicKey(): PublicKey {
        val response = http.get<GetPublicKeyResponse>("/public_key")
        return response.publicKey
    }

    suspend fun storeRecoveryParts(token: String, id: String, parts: List<ByteArray>) {
        parts.launchIndexed { index, bytes ->
            http.post<StoreRecoveryPartRequest, StoreRecoveryPartResponse>(
                "/storage/$id-$index",
                StoreRecoveryPartRequest(data = bytes),
                listOf(Token(token)),
            )
        }
    }

    suspend fun checkRecoveryParts(token: String, id: String, partsCount: Int): List<Boolean> =
        (0 until partsCount).toList().async {
            runCatching {
                http.head<CheckRecoveryPartResponse>(
                    "/storage/$id-$it",
                    listOf(Token(token))
                )
            }.isSuccess
        }

    suspend fun retrieveRecoveryParts(token: String, id: String, partsCount: Int): List<Result<ByteArray>> =
        (0 until partsCount).toList().async {
            runCatching {
                val response = http.get<RetrieveRecoveryPartResponse>(
                    "/storage/$id-$it",
                    listOf(Token(token))
                )
                response.data
            }
        }
}
