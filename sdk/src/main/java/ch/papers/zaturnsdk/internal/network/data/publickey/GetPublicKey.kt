package ch.papers.zaturnsdk.internal.network.data.publickey

import ch.papers.zaturnsdk.internal.network.serializer.Base64ByteArraySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class GetPublicKeyResponse(
    @Serializable(with = Base64ByteArraySerializer::class)
    @SerialName("public_key")
    val publicKey: ByteArray
)