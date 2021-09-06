package ch.papers.zaturnsdk.internal.oauth.google

import android.content.Context
import android.content.Intent
import ch.papers.zaturnsdk.data.OAuthId
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import kotlinx.coroutines.CompletableDeferred

internal class GoogleOAuth private constructor() {
    private val idDeferred: MutableMap<String, CompletableDeferred<OAuthId?>> = mutableMapOf()
    fun idDeferred(serverClientId: String): CompletableDeferred<OAuthId?>? = idDeferred[serverClientId]

    suspend fun signIn(context: Context, clientId: String, serverClientId: String, scopes: List<String>, nonce: String): OAuthId =
        withCredentialDeferred(serverClientId) {
            appAuthSignIn(context, clientId, serverClientId, scopes, nonce)
            it.await()?: failWithMissingToken()
        }

    private fun appAuthSignIn(context: Context, clientId: String, serverClientId: String, scopes: List<String>, nonce: String) {
        val intent = Intent(context, AppAuthActivity::class.java).apply {
            putExtra(AppAuthActivity.EXTRA_CLIENT_ID, clientId)
            putExtra(AppAuthActivity.EXTRA_SERVER_CLIENT_ID, serverClientId)
            putExtra(AppAuthActivity.EXTRA_SCOPES, ArrayList(scopes))
            putExtra(AppAuthActivity.EXTRA_NONCE, nonce)
        }

        context.startActivity(intent)
    }

    private fun oneTapSignIn(context: Context, serverClientId: String, scopes: List<String>, nonce: String) {
        val intent = Intent(context, GoogleSignInActivity::class.java).apply {
            putExtra(GoogleSignInActivity.EXTRA_SERVER_CLIENT_ID, serverClientId)
            putExtra(GoogleSignInActivity.EXTRA_SCOPES, ArrayList(scopes))
            putExtra(GoogleSignInActivity.EXTRA_NONCE, nonce)
        }
        context.startActivity(intent)
    }

    private inline fun <T> withCredentialDeferred(serverClientId: String, action: (CompletableDeferred<OAuthId?>) -> T): T {
        val deferred = CompletableDeferred<OAuthId?>()
        idDeferred[serverClientId] = deferred
        val result = action(deferred)
        idDeferred.remove(serverClientId)

        return result
    }

    companion object {
        private var instance: GoogleOAuth? = null
        fun instance(): GoogleOAuth = instance ?: GoogleOAuth().also { instance = it }
    }
}

private fun failWithMissingToken(): Nothing =
    throw OAuthException("Google ID token is missing from credential.")
