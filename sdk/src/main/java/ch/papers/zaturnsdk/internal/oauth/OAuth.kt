package ch.papers.zaturnsdk.internal.oauth

import android.content.Context
import ch.papers.zaturnsdk.data.OAuthProvider
import ch.papers.zaturnsdk.internal.crypto.data.PublicKey
import ch.papers.zaturnsdk.internal.oauth.apple.AppleOAuth
import ch.papers.zaturnsdk.internal.oauth.google.GoogleOAuth

internal class OAuth(
    private val appleOAuth: AppleOAuth = AppleOAuth.instance(),
    private val googleOAuth: GoogleOAuth = GoogleOAuth.instance()
) {
    suspend fun signIn(context: Context, nonce: String, provider: OAuthProvider): String =
        when (provider) {
            is OAuthProvider.Apple -> appleOAuth.signIn(
                context,
                provider.clientId,
                provider.serverClientId,
                provider.redirectUri,
                provider.responseTypes,
                provider.responseMode,
                provider.scopes,
                nonce
            )
            is OAuthProvider.Google -> googleOAuth.signIn(
                context,
                provider.clientId,
                provider.serverClientId,
                provider.scopes,
                nonce
            )
        }
}