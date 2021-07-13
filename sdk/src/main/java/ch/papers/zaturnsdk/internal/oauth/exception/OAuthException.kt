package ch.papers.zaturnsdk.internal.oauth.exception

import ch.papers.zaturnsdk.internal.exception.ZaturnInternalException

internal class OAuthException(message: String? = null, cause: Throwable? = null) : ZaturnInternalException(message, cause) {
    override val module: String = "OAuth"
}