package ch.papers.zaturnsdk.internal.secret.exception

import ch.papers.zaturnsdk.internal.exception.ZaturnInternalException

internal class SecretException(message: String? = null, cause: Throwable? = null) : ZaturnInternalException(message, cause) {
    override val module: String = "Secret"
}