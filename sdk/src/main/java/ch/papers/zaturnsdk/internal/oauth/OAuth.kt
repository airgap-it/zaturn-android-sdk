package ch.papers.zaturnsdk.internal.oauth

import android.content.Context
import ch.papers.zaturnsdk.data.OAuthProvider
import ch.papers.zaturnsdk.internal.crypto.data.PublicKey
import ch.papers.zaturnsdk.internal.oauth.apple.AppleOAuth
import ch.papers.zaturnsdk.internal.oauth.google.GoogleOAuth

internal interface OAuth {
    suspend fun signIn(context: Context, publicKey: PublicKey): String
}

internal fun OAuth(provider: OAuthProvider): OAuth =
    when (provider) {
        is OAuthProvider.Apple -> AppleOAuth()
        is OAuthProvider.Google -> GoogleOAuth(provider)
    }