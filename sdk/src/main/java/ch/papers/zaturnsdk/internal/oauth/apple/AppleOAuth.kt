package ch.papers.zaturnsdk.internal.oauth.apple

import android.content.Context
import android.content.Intent
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import kotlinx.coroutines.CompletableDeferred

internal class AppleOAuth {
    private val idTokenDeferred: MutableMap<String, CompletableDeferred<String?>> = mutableMapOf()
    fun idTokenDeferred(serverClientId: String): CompletableDeferred<String?>? =
        idTokenDeferred[serverClientId]

    suspend fun signIn(
        context: Context,
        clientId: String,
        serverClientId: String,
        redirectUri: String,
        responseTypes: List<String>,
        responseMode: String,
        scopes: List<String>,
        nonce: String,
    ): String =
        withCredentialDeferred(clientId) {
            appAuthSignIn(context, clientId, serverClientId, redirectUri, responseTypes, responseMode, scopes, nonce)
            it.await() ?: failWithMissingToken()
        }

    private fun appAuthSignIn(
        context: Context,
        clientId: String,
        serverClientId: String,
        redirectUri: String,
        responseTypes: List<String>,
        responseMode: String,
        scopes: List<String>,
        nonce: String,
    ) {
        val intent = Intent(context, AppAuthActivity::class.java).apply {
            putExtra(AppAuthActivity.EXTRA_CLIENT_ID, clientId)
            putExtra(AppAuthActivity.EXTRA_SERVER_CLIENT_ID, serverClientId)
            putExtra(AppAuthActivity.EXTRA_REDIRECT_URI, redirectUri)
            putExtra(AppAuthActivity.EXTRA_RESPONSE_TYPES, ArrayList(responseTypes))
            putExtra(AppAuthActivity.EXTRA_RESPONSE_MODE, responseMode)
            putExtra(AppAuthActivity.EXTRA_SCOPES, ArrayList(scopes))
            putExtra(AppAuthActivity.EXTRA_NONCE, nonce)
        }

        context.startActivity(intent)
    }

    private inline fun <T> withCredentialDeferred(
        serverClientId: String,
        action: (CompletableDeferred<String?>) -> T
    ): T {
        val deferred = CompletableDeferred<String?>()
        idTokenDeferred[serverClientId] = deferred
        val result = action(deferred)
        idTokenDeferred.remove(serverClientId)

        return result
    }

    companion object {
        private var instance: AppleOAuth? = null
        fun instance(): AppleOAuth = instance ?: AppleOAuth().also { instance = it }
    }
}

private fun failWithMissingToken(): Nothing =
    throw OAuthException("Apple ID token is missing from credential.")