package ch.papers.zaturnsdk.internal.oauth.google

import android.content.Context
import android.content.Intent
import ch.papers.zaturnsdk.data.OAuthProvider
import ch.papers.zaturnsdk.internal.oauth.OAuth
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import com.google.android.gms.auth.api.identity.SignInCredential
import kotlinx.coroutines.CompletableDeferred

internal class GoogleOAuth private constructor() {
    private val idTokenDeferred: MutableMap<String, CompletableDeferred<String?>> = mutableMapOf()
    fun idTokenDeferred(serverClientId: String): CompletableDeferred<String?>? = idTokenDeferred[serverClientId]

    suspend fun signIn(context: Context, clientId: String, serverClientId: String, nonce: String): String =
        withCredentialDeferred(serverClientId) {
            appAuthSignIn(context, clientId, serverClientId, nonce)
            it.await()?: failWithMissingToken()
        }

    private fun appAuthSignIn(context: Context, clientId: String, serverClientId: String, nonce: String) {
        val intent = Intent(context, AppAuthActivity::class.java).apply {
            putExtra(AppAuthActivity.EXTRA_CLIENT_ID, clientId)
            putExtra(AppAuthActivity.EXTRA_SERVER_CLIENT_ID, serverClientId)
            putExtra(AppAuthActivity.EXTRA_NONCE, nonce)
        }

        context.startActivity(intent)
    }

    private fun oneTapSignIn(context: Context, serverClientId: String, nonce: String) {
        val intent = Intent(context, GoogleSignInActivity::class.java).apply {
            putExtra(GoogleSignInActivity.EXTRA_SERVER_CLIENT_ID, serverClientId)
            putExtra(GoogleSignInActivity.EXTRA_NONCE, nonce)
        }
        context.startActivity(intent)
    }

    private inline fun <T> withCredentialDeferred(serverClientId: String, action: (CompletableDeferred<String?>) -> T): T {
        val deferred = CompletableDeferred<String?>()
        idTokenDeferred[serverClientId] = deferred
        val result = action(deferred)
        idTokenDeferred.remove(serverClientId)

        return result
    }

    companion object {
        private var instance: GoogleOAuth? = null
        fun instance(): GoogleOAuth = instance ?: GoogleOAuth().also { instance = it }
    }
}

private fun failWithMissingToken(): Nothing =
    throw OAuthException("Google ID token is missing from credential.")
