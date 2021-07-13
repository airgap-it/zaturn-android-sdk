package ch.papers.zaturnsdk.internal.crypto

import ch.papers.zaturnsdk.internal.crypto.data.SessionKey
import ch.papers.zaturnsdk.internal.crypto.data.KeyPair
import ch.papers.zaturnsdk.internal.crypto.data.PrivateKey
import ch.papers.zaturnsdk.internal.crypto.data.PublicKey
import ch.papers.zaturnsdk.internal.crypto.exception.CryptoException

internal interface Crypto {
    @Throws(CryptoException::class)
    fun keyPair(seed: ByteArray? = null): KeyPair

    @Throws(CryptoException::class)
    fun sessionKey(privateKey: PrivateKey, publicKey: PublicKey): SessionKey

    @Throws(CryptoException::class)
    fun encryptWithSessionKey(message: ByteArray, key: SessionKey): ByteArray

    @Throws(CryptoException::class)
    fun decryptWithSessionKey(message: ByteArray, key: SessionKey): ByteArray
}