package ch.papers.zaturnsdk.internal.oauth.google

import android.content.Context
import android.content.Intent
import ch.papers.zaturnsdk.data.OAuthProvider
import ch.papers.zaturnsdk.internal.crypto.data.PublicKey
import ch.papers.zaturnsdk.internal.oauth.OAuth
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import ch.papers.zaturnsdk.internal.util.encodeToBase64
import com.google.android.gms.auth.api.identity.SignInCredential
import kotlinx.coroutines.CompletableDeferred

internal class GoogleOAuth(private val serverClientId: String) : OAuth {
    var credentialDeferred: CompletableDeferred<SignInCredential>? = null
        private set

    override suspend fun signIn(context: Context, publicKey: PublicKey): String =
        withCredentialDeferred {
            oneTapSignIn(context, publicKey)
            it.await().googleIdToken ?: failWithMissingToken()
        }

    private fun oneTapSignIn(context: Context, publicKey: PublicKey) {
        val intent = Intent(context, GoogleOAuthActivity::class.java).apply {
            putExtra(GoogleOAuthActivity.EXTRA_SERVER_CLIENT_ID, serverClientId)
            putExtra(GoogleOAuthActivity.EXTRA_NONCE, publicKey.encodeToBase64())
        }
        context.startActivity(intent)
    }

    private inline fun <T> withCredentialDeferred(action: (CompletableDeferred<SignInCredential>) -> T): T {
        val deferred = CompletableDeferred<SignInCredential>()
        credentialDeferred = deferred
        val result = action(deferred)
        credentialDeferred = null

        return result
    }

    companion object {
        private var _instances: MutableMap<String, GoogleOAuth> = mutableMapOf()
        fun instance(serverClientId: String): GoogleOAuth =
            _instances.getOrPut(serverClientId) { GoogleOAuth(serverClientId) }
    }
}

internal fun GoogleOAuth(configuration: OAuthProvider.Google): GoogleOAuth =
    with(configuration) { GoogleOAuth.instance(serverClientId) }

private fun failWithMissingToken(): Nothing =
    throw OAuthException("Google ID token is missing from credential.")
