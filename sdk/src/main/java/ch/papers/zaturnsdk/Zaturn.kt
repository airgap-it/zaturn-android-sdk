package ch.papers.zaturnsdk

import android.content.Context
import androidx.annotation.IntRange
import ch.papers.zaturnsdk.data.OAuthProvider
import ch.papers.zaturnsdk.exception.ZaturnException
import ch.papers.zaturnsdk.internal.ZaturnConfiguration
import ch.papers.zaturnsdk.internal.crypto.Crypto
import ch.papers.zaturnsdk.internal.crypto.data.KeyPair
import ch.papers.zaturnsdk.internal.crypto.data.SessionKey
import ch.papers.zaturnsdk.internal.crypto.sodium.SodiumCrypto
import ch.papers.zaturnsdk.internal.exception.ZaturnInternalException
import ch.papers.zaturnsdk.internal.network.ZaturnNode
import ch.papers.zaturnsdk.internal.network.http.ktor.KtorHttp
import ch.papers.zaturnsdk.internal.oauth.OAuth
import ch.papers.zaturnsdk.internal.secret.SecretSharing
import ch.papers.zaturnsdk.internal.secret.SecretSharingGroup
import ch.papers.zaturnsdk.internal.secret.sskr.SskrSecretSharing
import ch.papers.zaturnsdk.internal.util.async
import ch.papers.zaturnsdk.internal.util.encodeToBase64
import ch.papers.zaturnsdk.internal.util.launch
import ch.papers.zaturnsdk.internal.util.url
import kotlin.math.min

public class Zaturn internal constructor(
    private val nodes: List<ZaturnNode>,
    private val oAuth: OAuth,
    private val crypto: Crypto,
    private val secretSharing: SecretSharing,
    private val shareConfiguration: ShareConfiguration,
) {
    private val keyPair: KeyPair by lazy { crypto.keyPair() }
    private val sessionKeys: MutableMap<String, SessionKey> = mutableMapOf()

    @get:Throws(ZaturnException::class)
    public val nonce: String
        get() = catchInternal { keyPair.publicKey.encodeToBase64() }

    @Throws(ZaturnException::class)
    public suspend fun getOAuthToken(context: Context, oAuthProvider: OAuthProvider): String =
        catchInternal { oAuth.signIn(context, nonce, oAuthProvider) }

    @Throws(ZaturnException::class)
    public suspend fun setupRecovery(id: String, secret: ByteArray, token: String) =
        catchInternal {
            val parts = splitSecret(secret)
            storeRecoveryParts(id, token, parts)
        }

    @Throws(ZaturnException::class)
    public suspend fun recover(id: String, token: String): ByteArray =
        catchInternal {
            val parts = retrieveRecoveryParts(id, token)
            return restoreSecret(parts)
        }

    private fun splitSecret(secret: ByteArray): List<List<ByteArray>> =
        secretSharing.split(secret, (0 until shareConfiguration.groups).map {
            SecretSharingGroup(
                shareConfiguration.groupMembers,
                shareConfiguration.groupMemberThreshold,
            )
        }, shareConfiguration.groupThreshold)

    private fun restoreSecret(parts: List<List<ByteArray>>): ByteArray =
        secretSharing.join(parts)

    private suspend fun sessionKey(node: ZaturnNode): SessionKey =
        sessionKeys.getOrPut(node.id) {
            val nodePublicKey = node.publicKey()
            crypto.sessionKey(keyPair.privateKey, nodePublicKey)
        }

    private suspend fun encryptParts(node: ZaturnNode, parts: List<ByteArray>): List<ByteArray> {
        val sessionKey = sessionKey(node)
        return parts.map { crypto.encryptWithSessionKey(it, sessionKey) }
    }

    private suspend fun decryptParts(node: ZaturnNode, parts: List<ByteArray>): List<ByteArray> {
        val sessionKey = sessionKey(node)
        return parts.map { crypto.decryptWithSessionKey(it, sessionKey) }
    }

    private suspend fun storeRecoveryParts(
        id: String,
        token: String,
        parts: List<List<ByteArray>>
    ) {
        nodes.zip(parts).launch { (node, parts) ->
            val encrypted = encryptParts(node, parts)
            node.storeRecoveryParts(token, id, encrypted)
        }
    }

    private suspend fun retrieveRecoveryParts(id: String, token: String): List<List<ByteArray>> =
        nodes.async {
            val encrypted = it.retrieveRecoveryParts(token, id, shareConfiguration.groupMembers)
            decryptParts(it, encrypted)
        }

    private inline fun <T> catchInternal(block: () -> T): T =
        try {
            block()
        } catch (e: ZaturnInternalException) {
            failWithInternalError(e)
        }

    internal data class ShareConfiguration(
        val groups: Int,
        val groupThreshold: Int,
        val groupMembers: Int,
        val groupMemberThreshold: Int
    )

    public class Builder(public val nodes: List<String>) {
        @IntRange(from = ZaturnConfiguration.MIN_GROUP_THRESHOLD)
        public var groupThreshold: Int? = null

        @IntRange(from = ZaturnConfiguration.MIN_GROUP_MEMBERS)
        public var groupMembers: Int? = null

        @IntRange(from = ZaturnConfiguration.MIN_GROUP_MEMBER_THRESHOLD)
        public var groupMemberThreshold: Int? = null

        @Throws(ZaturnException::class)
        public fun build(): Zaturn {
            val nodes = nodes.map {
                val http = KtorHttp(url(it, ZaturnConfiguration.API))
                ZaturnNode(it, http)
            }
            val oAuth = OAuth()
            val crypto = SodiumCrypto()
            val secretSharing = SskrSecretSharing()
            val shareConfiguration = ShareConfiguration(
                groups = min(nodes.size, ZaturnConfiguration.MIN_GROUPS.toInt()),
                groupThreshold = groupThreshold ?: min(
                    (nodes.size / 2) + 1,
                    ZaturnConfiguration.MIN_GROUP_THRESHOLD.toInt()
                ),
                groupMembers = groupMembers ?: ZaturnConfiguration.MIN_GROUP_MEMBERS.toInt(),
                groupMemberThreshold = groupMemberThreshold
                    ?: ZaturnConfiguration.MIN_GROUP_MEMBER_THRESHOLD.toInt(),
            ).also(this::validateConfiguration)

            return Zaturn(nodes, oAuth, crypto, secretSharing, shareConfiguration)
        }

        @Throws(ZaturnException::class)
        private fun validateConfiguration(configuration: ShareConfiguration) = with(configuration) {
            if (groupThreshold > groups) failWithGroupThresholdExceeded(groups, groupMemberThreshold)
            if (groupMemberThreshold > groupMembers) failWithMemberThresholdExceeded(
                groupMembers,
                groupMemberThreshold
            )
        }
    }

    public companion object {}
}

@Throws(ZaturnException::class)
public fun Zaturn(nodes: List<String>, builderAction: Zaturn.Builder.() -> Unit = {}): Zaturn =
    Zaturn.Builder(nodes).also(builderAction).build()

internal fun failWithInternalError(exception: ZaturnInternalException): Nothing =
    throw ZaturnException("Internal error: $exception")

internal fun failWithGroupThresholdExceeded(groups: Int, threshold: Int): Nothing =
    throw ZaturnException("The group threshold ($threshold) exceeds the number of groups ($groups).")

internal fun failWithMemberThresholdExceeded(members: Int, threshold: Int): Nothing =
    throw ZaturnException("The member threshold ($threshold) exceeds the number of members ($members).")