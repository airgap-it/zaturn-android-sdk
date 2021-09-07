package ch.papers.zaturnsdk.internal.util

import ch.papers.zaturnsdk.data.OAuthId
import com.google.android.gms.auth.api.identity.SignInCredential

internal fun SignInCredential.toOAuthId(): OAuthId? {
    val idToken = googleIdToken ?: return null

    return OAuthId(
        idToken = idToken,
        additional = mapOf(
            "displayName" to displayName,
            "givenName" to givenName,
            "familyName" to familyName,
        ).filterValuesNotNull()
    )
}