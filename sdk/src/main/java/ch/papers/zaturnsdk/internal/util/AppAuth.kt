package ch.papers.zaturnsdk.internal.util

import ch.papers.zaturnsdk.data.OAuthId
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.TokenResponse

internal fun AuthorizationRequest.Builder.setScope(scopes: Set<String>): AuthorizationRequest.Builder =
    setScope(scopes.joinToString(" "))

internal fun AuthorizationRequest.Builder.setResponseType(responseTypes: Set<String>): AuthorizationRequest.Builder =
    setResponseType(responseTypes.joinToString(" "))

internal fun AuthorizationResponse.toOAuthId(): OAuthId? {
    val accessToken = accessToken ?: return null
    val expiresIn = accessTokenExpirationTime ?: return null
    val idToken = idToken ?: return null
    val scope = scope ?: return null
    val tokenType = tokenType ?: return null

    return OAuthId(
        accessToken,
        expiresIn,
        idToken,
        scope,
        tokenType,
        refreshToken = null,
        additionalParameters,
    )
}

internal fun TokenResponse.toOAuthId(): OAuthId? {
    val accessToken = accessToken ?: return null
    val expiresIn = accessTokenExpirationTime ?: return null
    val idToken = idToken ?: return null
    val scope = scope ?: return null
    val tokenType = tokenType ?: return null

    return OAuthId(
        accessToken,
        expiresIn,
        idToken,
        scope,
        tokenType,
        refreshToken,
        additionalParameters,
    )
}