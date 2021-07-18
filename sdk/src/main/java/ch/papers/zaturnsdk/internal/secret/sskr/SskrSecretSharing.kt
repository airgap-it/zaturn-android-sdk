package ch.papers.zaturnsdk.internal.secret.sskr

import androidx.annotation.IntRange
import ch.papers.zaturnsdk.internal.secret.SecretSharing
import ch.papers.zaturnsdk.internal.secret.SecretSharingGroup
import ch.papers.zaturnsdk.internal.secret.exception.SecretException
import okhttp3.internal.and
import kotlin.random.Random
import kotlin.random.nextUInt

internal class SskrSecretSharing(private val shamir: Shamir = Shamir()) : SecretSharing {
    override fun split(
        secret: ByteArray,
        groups: List<SecretSharingGroup>,
        @IntRange(from = 1) groupThreshold: Int
    ): List<List<ByteArray>> {
        if (secret.size > MAX_SECRET_SIZE) failWithSecretSizeInvalid(secret.size)
        if (groups.size > MAX_GROUPS) failWithGroupSizeUnsupported(MAX_GROUPS, groups.size)
        if (groupThreshold > groups.size) failWithGroupThresholdInvalid(groupThreshold, groups.size)

        val identifier = Random.nextUInt().toUShort()
        val shards = shamir.split(secret, groups.size, groupThreshold)

        return shards.mapIndexed { index, bytes ->
            val (members, memberThreshold) = groups[index]

            GroupShard(
                identifier,
                groupThreshold,
                groups.size,
                index,
                memberThreshold,
                split(bytes, members, memberThreshold),
            ).serialized()
        }
    }

    override fun join(parts: List<List<ByteArray>>): ByteArray {
        val shards = parts.mapNotNull { bytes ->
            val group = GroupShard.deserialized(bytes) ?: return@mapNotNull null
            val members = group.members.sortedBy { it.memberIndex }.map { it.shareValue }
            shamir.join(members)
        }

        return shamir.join(shards)
    }

    private fun split(
        secret: ByteArray,
        members: Int,
        memberThreshold: Int,
    ): List<MemberShard> {
        val shares = shamir.split(secret, members, memberThreshold)
        return shares.mapIndexed { index, bytes -> MemberShard(index, bytes) }
    }

    private class MemberShard(
        val memberIndex: Int,
        val shareValue: ByteArray
    ) {
        fun serialized(): ByteArray {
            val memberIndexMasked = memberIndex and 0xf
            return byteArrayOf(memberIndexMasked.toByte()) + shareValue
        }

        companion object {
            fun deserialized(bytes: ByteArray): MemberShard {
                val memberIndex = bytes[0] and 0xf
                val shareValue = bytes.sliceArray(1 until bytes.size)

                return MemberShard(memberIndex, shareValue)
            }
        }
    }

    private class GroupShard(
        val identifier: UShort,
        val groupThreshold: Int,
        val groupCount: Int,
        val groupIndex: Int,
        val memberThreshold: Int,
        val members: List<MemberShard>
    ) {
        fun serialized(): List<ByteArray> = members.map {
            val identifierMasked = identifier.toShort() and 0xffff
            val groupThresholdMasked = (groupThreshold - 1) and 0xf
            val groupCountMasked = (groupCount - 1) and 0xf
            val groupIndexMasked = groupIndex and 0xf
            val memberThresholdMasked = (memberThreshold - 1) and 0xf
            val memberSerialized = it.serialized()

            byteArrayOf(
                (identifierMasked shr 8).toByte(),
                (identifierMasked and 0xff).toByte(),
                ((groupThresholdMasked shl 4) or groupCountMasked).toByte(),
                ((groupIndexMasked shl 4) or memberThresholdMasked).toByte(),
            ) + memberSerialized
        }

        fun matches(other: GroupShard): Boolean =
            other.identifier == identifier && other.groupThreshold == groupThreshold && other.groupCount == groupCount && other.groupIndex == groupIndex && other.memberThreshold == memberThreshold

        fun copy(
            identifier: UShort = this.identifier,
            groupThreshold: Int = this.groupThreshold,
            groupCount: Int = this.groupCount,
            groupIndex: Int = this.groupIndex,
            memberThreshold: Int = this.memberThreshold,
            members: List<MemberShard> = this.members,
        ): GroupShard =
            GroupShard(identifier, groupThreshold, groupCount, groupIndex, memberThreshold, members)

        companion object {
            fun deserialized(bytes: List<ByteArray>): GroupShard? =
                bytes.map {
                    val groupThreshold = (it[2].toInt() shr 4) + 1
                    val groupCount = (it[2] and 0xf) + 1
                    if (groupThreshold > groupCount) failWithGroupThresholdInvalid(
                        groupThreshold,
                        groupCount
                    )

                    val reserved = it[4].toInt() shr 4
                    if (reserved != 0) failWithReservedBitInvalid()

                    val identifier = ((it[0].toInt() shl 8) or it[1].toInt()).toUShort()
                    val groupIndex = (it[3].toInt() shr 4)
                    val memberThreshold = (it[3] and 0xf) + 1
                    val member = MemberShard.deserialized(it.sliceArray(4 until it.size))

                    GroupShard(
                        identifier,
                        groupThreshold,
                        groupCount,
                        groupIndex,
                        memberThreshold,
                        listOf(member)
                    )
                }.flatten()

            private fun List<GroupShard>.flatten(): GroupShard? =
                firstOrNull()?.let {
                    slice(1 until size).fold(it) { acc, next ->
                        if (!next.matches(acc)) failWithGroupShardsMismatch()
                        acc.copy(members = acc.members + next.members)
                    }
                }
        }
    }

    companion object {
        private const val MAX_SECRET_SIZE = Byte.MAX_VALUE
        private const val MAX_GROUPS = 16
    }
}

private fun failWithSecretSizeInvalid(size: Int): Nothing =
    throw SecretException("Secret size not supported, expected not larger than $size bytes but got $size bytes.")

private fun failWithGroupSizeUnsupported(max: Int, size: Int): Nothing =
    throw SecretException("Group size not supported, expected not more than ${max} but got $size.")

private fun failWithGroupThresholdInvalid(threshold: Int, max: Int): Nothing =
    throw SecretException("Group threshold ($threshold) exceeded groups length ($max).")

private fun failWithReservedBitInvalid(): Nothing =
    throw SecretException("Invalid reserved bit.")

private fun failWithGroupShardsMismatch(): Nothing =
    throw SecretException("Shards does not belong to the same group.")