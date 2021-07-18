package ch.papers.zaturnsdk.internal.network.data.storage

import ch.papers.zaturnsdk.internal.network.serializer.Base64ByteArraySerializer
import kotlinx.serialization.Serializable

@Serializable
internal class StoreRecoveryPartRequest(
    @Serializable(with = Base64ByteArraySerializer::class)
    val data: ByteArray
)

internal typealias StoreRecoveryPartResponse = Unit