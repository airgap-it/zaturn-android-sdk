package ch.papers.zaturnsdk

import ch.papers.zaturnsdk.internal.secret.SecretSharingGroup
import ch.papers.zaturnsdk.internal.secret.sskr.SskrSecretSharing
import org.junit.Test

internal class SecretSharingTest {
    @Test
    fun test() {
        val sskr = SskrSecretSharing()
        val secret = bytes("fa8f33a284859498848763ec45b53a92")
        val shards = sskr.split(secret, listOf(
            SecretSharingGroup(2, 2),
            SecretSharingGroup(2, 2),
        ), 2)
        val joined = sskr.join(shards)
        println(hexString(joined))
    }

    fun bytes(string: String): ByteArray = string.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    fun hexString(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}