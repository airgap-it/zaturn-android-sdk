package ch.papers.zaturnsdk.internal.crypto.exception

import ch.papers.zaturnsdk.internal.exception.ZaturnInternalException

internal class CryptoException(message: String? = null, cause: Throwable? = null) : ZaturnInternalException(message, cause) {
    override val module: String = "Crypto"
}