package ch.papers.zaturnsdk.internal.crypto.sodium

import ch.papers.zaturnsdk.internal.crypto.Crypto
import ch.papers.zaturnsdk.internal.crypto.data.KeyPair
import ch.papers.zaturnsdk.internal.crypto.data.PrivateKey
import ch.papers.zaturnsdk.internal.crypto.data.PublicKey
import ch.papers.zaturnsdk.internal.crypto.data.SessionKey
import ch.papers.zaturnsdk.internal.crypto.exception.CryptoException
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.Random

internal class SodiumCrypto : Crypto {
    private val sodium: LazySodiumAndroid by lazy { LazySodiumAndroid(SodiumAndroid()) }

    @Throws(CryptoException::class)
    override fun keyPair(seed: ByteArray?): KeyPair {
        val box = sodium as Box.Native

        val privateKey = ByteArray(Box.SECRETKEYBYTES)
        val publicKey = ByteArray(Box.PUBLICKEYBYTES)

        assertOrFail(::failWithKeyPairNotGenerated) {
            box.cryptoBoxKeypair(publicKey, privateKey)
        }

        return KeyPair(privateKey, publicKey)
    }

    @Throws(CryptoException::class)
    override fun sessionKey(privateKey: PrivateKey, publicKey: PublicKey): SessionKey {
        if (privateKey.size != Box.SECRETKEYBYTES) failWithPrivateKeyInvalid()
        if (publicKey.size != Box.PUBLICKEYBYTES) failWithPublicKeyInvalid()

        val box = sodium as Box.Native

        return ByteArray(Box.BEFORENMBYTES).also {
            assertOrFail(::failWithSessionKeyNotGenerated) {
                box.cryptoBoxBeforeNm(it, publicKey, privateKey)
            }
        }
    }

    @Throws(CryptoException::class)
    override fun encryptWithSessionKey(message: ByteArray, key: SessionKey): ByteArray {
        if (key.size != Box.BEFORENMBYTES) failWithSessionKeyInvalid()

        val box = sodium as Box.Native
        val random = sodium as Random

        val nonce = random.randomBytesBuf(Box.NONCEBYTES)

        val ciphertext = ByteArray(Box.MACBYTES + message.size).also {
            assertOrFail(::failWithEncryptionFailed) {
                box.cryptoBoxEasyAfterNm(it, message, message.size.toLong(), nonce, key)
            }
        }

        return nonce + ciphertext
    }

    @Throws(CryptoException::class)
    override fun decryptWithSessionKey(message: ByteArray, key: SessionKey): ByteArray {
        if (key.size != Box.BEFORENMBYTES) failWithSessionKeyInvalid()

        val box = sodium as Box.Native

        val nonce = message.sliceArray(0 until Box.NONCEBYTES)
        val ciphertext = message.sliceArray(Box.NONCEBYTES until message.size)

        return ByteArray(ciphertext.size - Box.MACBYTES).also {
            assertOrFail(::failWithDecryptionFailed) {
                box.cryptoBoxOpenEasyAfterNm(it, ciphertext, ciphertext.size.toLong(), nonce, key)
            }
        }
    }

    private inline fun assertOrFail(exception: () -> Nothing, block: () -> Boolean) {
        if (!block()) throw exception()
    }
}

private fun failWithKeyPairNotGenerated(): Nothing =
    throw CryptoException("Key pair could not be generated.")

private fun failWithPrivateKeyInvalid(): Nothing =
    throw CryptoException("Invalid private key.")

private fun failWithPublicKeyInvalid(): Nothing =
    throw CryptoException("Invalid public key.")

private fun failWithSessionKeyInvalid(): Nothing =
    throw CryptoException("Invalid session key.")

private fun failWithSessionKeyNotGenerated(): Nothing =
    throw CryptoException("Session key could not be generated.")

private fun failWithEncryptionFailed(): Nothing =
    throw CryptoException("The message could not be encrypted with the session key.")

private fun failWithDecryptionFailed(): Nothing =
    throw CryptoException("The message could not be decrypted with the session key.")