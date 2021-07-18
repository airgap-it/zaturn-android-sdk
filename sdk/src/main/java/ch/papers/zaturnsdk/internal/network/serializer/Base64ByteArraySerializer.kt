package ch.papers.zaturnsdk.internal.network.serializer

import ch.papers.zaturnsdk.internal.util.decodeFromBase64
import ch.papers.zaturnsdk.internal.util.encodeToBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object Base64ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64 = decoder.decodeString()
        return base64.decodeFromBase64()
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64 = value.encodeToBase64()
        encoder.encodeString(base64)
    }
}