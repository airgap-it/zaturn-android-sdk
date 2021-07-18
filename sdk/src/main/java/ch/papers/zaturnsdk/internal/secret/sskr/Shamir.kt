package ch.papers.zaturnsdk.internal.secret.sskr

import com.codahale.shamir.Scheme
import java.security.SecureRandom

internal class Shamir {
    private val secureRandom: SecureRandom
        get() = SecureRandom()

    fun split(secret: ByteArray, parts: Int, threshold: Int): List<ByteArray> {
        if (parts == 1) return listOf(secret)

        val shamir = shamir(parts, threshold)
        val split = shamir.split(secret)

        return split.entries.sortedBy { it.key }.map { it.value }
    }

    fun join(parts: List<ByteArray>): ByteArray {
        if (parts.size == 1) return parts.first()

        val shamir = shamir()
        val split = parts.mapIndexed { index, bytes -> index + 1 to bytes }.toMap()

        return shamir.join(split)
    }

    private fun shamir(parts: Int = 2, minRequired: Int = 2): Scheme = Scheme(secureRandom, parts, minRequired)
}