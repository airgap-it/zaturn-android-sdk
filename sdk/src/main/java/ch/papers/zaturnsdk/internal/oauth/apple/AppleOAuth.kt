package ch.papers.zaturnsdk.internal.oauth.apple

import android.content.Context
import ch.papers.zaturnsdk.internal.crypto.data.PublicKey
import ch.papers.zaturnsdk.internal.oauth.OAuth

internal class AppleOAuth : OAuth {
    override suspend fun signIn(context: Context, publicKey: PublicKey): String {
        TODO("Not yet implemented")
    }
}