package ch.papers.zaturnsdk.internal.util

import net.openid.appauth.AuthorizationRequest

internal fun AuthorizationRequest.Builder.setScope(scopes: Set<String>): AuthorizationRequest.Builder =
    setScope(scopes.joinToString(" "))

internal fun AuthorizationRequest.Builder.setResponseType(responseTypes: Set<String>): AuthorizationRequest.Builder =
    setResponseType(responseTypes.joinToString(" "))