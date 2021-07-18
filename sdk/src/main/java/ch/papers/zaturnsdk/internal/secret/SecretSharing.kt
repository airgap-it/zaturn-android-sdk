package ch.papers.zaturnsdk.internal.secret

import androidx.annotation.IntRange

internal data class SecretSharingGroup(val members: Int, @IntRange(from = 1) val memberThreshold: Int)

internal interface SecretSharing {
    fun split(
        secret: ByteArray,
        groups: List<SecretSharingGroup> = listOf(
            SecretSharingGroup(members = 2, memberThreshold = 2),
            SecretSharingGroup(members = 2, memberThreshold = 2),
        ),
        @IntRange(from = 1) groupThreshold: Int = 2,
    ): List<List<ByteArray>>
    fun join(parts: List<List<ByteArray>>): ByteArray
}