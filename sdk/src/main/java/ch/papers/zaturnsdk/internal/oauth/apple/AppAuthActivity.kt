package ch.papers.zaturnsdk.internal.oauth.apple

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.papers.zaturnsdk.data.OAuthId
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import ch.papers.zaturnsdk.internal.util.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.*
import kotlin.coroutines.resume

internal class AppAuthActivity : AppCompatActivity() {
    private var idDeferred: CompletableDeferred<OAuthId?>? = null

    private val disposableHandles: MutableList<DisposableHandle> = mutableListOf()

    private val authorizationService: AuthorizationService by lazy { AuthorizationService(this) }

    private val authorizationEndpoint: Uri
        get() = Uri.parse(AUTHORIZATION_ENDPOINT)

    private val tokenEndpoint: Uri
        get() = Uri.parse(TOKEN_ENDPOINT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientId = intent.extras?.getString(EXTRA_CLIENT_ID) ?: failWithMissingClientId()
        val serverClientId = intent.extras?.getString(EXTRA_SERVER_CLIENT_ID) ?: failWithMissingServerClientId()
        val redirectUri = intent.extras?.getString(EXTRA_REDIRECT_URI) ?: failWithMissingRedirectUri()
        val responseTypes = intent.extras?.getStringArrayList(EXTRA_RESPONSE_TYPES) ?: failWithMissingResponseTypes()
        val responseMode = intent.extras?.getString(EXTRA_RESPONSE_MODE) ?: failWithMissingResponseMode()
        val scopes = intent.extras?.getStringArrayList(EXTRA_SCOPES) ?: emptyList<String>()
        val nonce = intent.extras?.getString(EXTRA_NONCE) ?: failWithMissingNonce()

        idDeferred = AppleOAuth.instance().idDeferred(clientId)

        disposableHandles.addNotNull(idDeferred?.invokeOnCompletion { finish() })
        idDeferred?.catch {
            appAuthSignIn(clientId, serverClientId, redirectUri, scopes, responseTypes, responseMode, nonce)
        }
    }

    override fun onDestroy() {
        disposableHandles.forEach { it.dispose() }
        super.onDestroy()
    }

    private fun appAuthSignIn(
        clientId: String,
        serverClientId: String,
        redirectUri: String,
        scopes: List<String>,
        responseTypes: List<String>,
        responseMode: String,
        nonce: String
    ) {
        val configuration = AuthorizationServiceConfiguration(authorizationEndpoint, tokenEndpoint)
        val authorizationRequest = AuthorizationRequest.Builder(
            configuration,
            serverClientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri),
        ).apply {
            val scopes = setOf(
                AuthorizationRequest.Scope.OPENID,
            ) + scopes.toSet()

            setScope(scopes)
            setResponseType(responseTypes.toSet())
            setResponseMode(responseMode)
            setNonce(nonce)
        }.build()

        authorize(clientId, serverClientId, authorizationRequest)
    }

    private fun authorize(clientId: String, serverClientId: String, request: AuthorizationRequest) {
        val intent = authorizationService.getAuthorizationRequestIntent(request)

        val authorize =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                idDeferred?.catch {
                    when (it.resultCode) {
                        Activity.RESULT_OK -> {
                            val data = it.data ?: failWithSignInFailure()

                            val response = AuthorizationResponse.fromIntent(data)
                            val exception = AuthorizationException.fromIntent(data)

                            if (exception != null || response == null) failWithSignInFailure(
                                exception
                            )

                            getId(clientId, serverClientId, response)
                        }
                        else -> failWithSignInFailure()
                    }
                }
            }

        authorize.launch(intent)
    }

    private fun getId(
        clientId: String,
        serverClientId: String,
        response: AuthorizationResponse
    ) {
        response.toOAuthId()?.let { idDeferred?.complete(it) } ?: exchangeToken(
            clientId,
            serverClientId,
            response
        )
    }

    private fun exchangeToken(
        clientId: String,
        serverClientId: String,
        response: AuthorizationResponse
    ) {
        val request = with(response) {
            TokenRequest.Builder(request.configuration, clientId).apply {
                val additionalParameters = mapOf(
                    PARAMETER_AUDIENCE to (request.additionalParameters[PARAMETER_AUDIENCE]
                        ?: serverClientId),
                    PARAMETER_CLIENT_SECRET to response.additionalParameters[PARAMETER_CLIENT_SECRET]
                ).filterValues { it != null }

                setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
                setRedirectUri(request.redirectUri)
                setCodeVerifier(request.codeVerifier)
                setAuthorizationCode(authorizationCode)
                setAdditionalParameters(additionalParameters)
                setNonce(request.nonce)
            }.build()
        }
        lifecycleScope.launch { exchangeToken(request) }
    }

    private suspend fun exchangeToken(request: TokenRequest) {
        idDeferred?.tryComplete {
            val id = suspendCancellableCoroutine<OAuthId?> {
                authorizationService.performTokenRequest(request) { response, exception ->
                    try {
                        if (exception != null || response == null) failWithSignInFailure(exception)
                        it.resume(response.toOAuthId())
                    } catch (e: Exception) {
                        it.cancel(e)
                    }
                }
            }

            id
        }
    }

    companion object {
        const val EXTRA_CLIENT_ID = "clientId"
        const val EXTRA_SERVER_CLIENT_ID = "serverClientId"
        const val EXTRA_REDIRECT_URI = "redirectUri"
        const val EXTRA_RESPONSE_TYPES = "responseTypes"
        const val EXTRA_RESPONSE_MODE = "responseMode"
        const val EXTRA_SCOPES = "scopes"
        const val EXTRA_NONCE = "nonce"

        private const val PARAMETER_AUDIENCE = "audience"
        private const val PARAMETER_CLIENT_SECRET = "client_secret"

        private const val AUTHORIZATION_ENDPOINT = "https://appleid.apple.com/auth/authorize"
        private const val TOKEN_ENDPOINT = "https://appleid.apple.com/auth/token"
    }
}

private fun failWithMissingClientId(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing clientId.")

private fun failWithMissingServerClientId(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing serverClientId.")

private fun failWithMissingRedirectUri(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing redirectUri.")

private fun failWithMissingResponseTypes(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing responseTypes.")

private fun failWithMissingResponseMode(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing responseMode.")

private fun failWithMissingNonce(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing nonce.")

private fun failWithSignInFailure(cause: Throwable? = null): Nothing {
    val details = cause?.let { ", caused by: $cause" } ?: "."
    throw OAuthException("Could not sign in with Apple$details")
}