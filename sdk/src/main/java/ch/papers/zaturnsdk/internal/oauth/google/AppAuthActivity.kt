package ch.papers.zaturnsdk.internal.oauth.google

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.papers.zaturnsdk.data.OAuthId
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import ch.papers.zaturnsdk.internal.util.*
import ch.papers.zaturnsdk.internal.util.addNotNull
import ch.papers.zaturnsdk.internal.util.catch
import ch.papers.zaturnsdk.internal.util.setScope
import ch.papers.zaturnsdk.internal.util.tryComplete
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

    private fun redirectUrl(clientId: String): Uri {
        val clientIdentifierScheme = clientId
            .split(".")
            .reversed()
            .joinToString(".")
        return Uri.parse("$clientIdentifierScheme:$OAUTH2_CALLBACK_PATH")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientId = intent.extras?.getString(EXTRA_CLIENT_ID) ?: failWithMissingClientId()
        val serverClientId = intent.extras?.getString(EXTRA_SERVER_CLIENT_ID) ?: failWithMissingServerClientId()
        val scopes = intent.extras?.getStringArrayList(EXTRA_SCOPES) ?: emptyList<String>()
        val nonce = intent.extras?.getString(EXTRA_NONCE) ?: failWithMissingNonce()

        idDeferred = GoogleOAuth.instance().idDeferred(serverClientId)

        disposableHandles.addNotNull(idDeferred?.invokeOnCompletion { finish() })
        idDeferred?.catch {
            appAuthSignIn(clientId, serverClientId, scopes, nonce)
        }
    }

    override fun onDestroy() {
        disposableHandles.forEach { it.dispose() }
        super.onDestroy()
    }

    private fun appAuthSignIn(clientId: String, serverClientId: String, scopes: List<String>, nonce: String) {
        val configuration = AuthorizationServiceConfiguration(authorizationEndpoint, tokenEndpoint)
        val authorizationRequest = AuthorizationRequest.Builder(
            configuration,
            clientId,
            ResponseTypeValues.CODE,
            redirectUrl(clientId),
        ).apply {
            val additionalParameters = mapOf(
                PARAMETER_AUDIENCE to serverClientId,
            )

            val scopes = setOf(
                AuthorizationRequest.Scope.OPENID,
                AuthorizationRequest.Scope.EMAIL,
            ) + scopes.toSet()

            setScope(scopes)
            setResponseType(ResponseTypeValues.CODE)
            setAdditionalParameters(additionalParameters)
            setNonce(nonce)
        }.build()

        authorize(authorizationRequest)
    }

    private fun authorize(request: AuthorizationRequest) {
        val intent = authorizationService.getAuthorizationRequestIntent(request)

        val authorize = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            idDeferred?.catch {
                when (it.resultCode) {
                    Activity.RESULT_OK -> {
                        val data = it.data ?: failWithSignInFailure()

                        val response = AuthorizationResponse.fromIntent(data)
                        val exception = AuthorizationException.fromIntent(data)

                        if (exception != null || response == null) failWithSignInFailure(exception)

                        getId(response)
                    }
                    else -> failWithSignInFailure()
                }
            }
        }

        authorize.launch(intent)
    }

    private fun getId(response: AuthorizationResponse) {
        response.toOAuthId()?.let { idDeferred?.complete(it) } ?: exchangeToken(response)
    }

    private fun exchangeToken(response: AuthorizationResponse) {
        val request = response.createTokenExchangeRequest(mapOf(
            PARAMETER_AUDIENCE to response.request.additionalParameters[PARAMETER_AUDIENCE],
        ).filterValues { it != null })
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
        const val EXTRA_SCOPES = "scopes"
        const val EXTRA_NONCE = "nonce"

        private const val AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

        private const val PARAMETER_AUDIENCE = "audience"

        private const val OAUTH2_CALLBACK_PATH = "/oauth2callback"
    }
}

private fun failWithMissingClientId(): Nothing =
    throw IllegalStateException("Could not sign in with Google, missing clientId.")

private fun failWithMissingServerClientId(): Nothing =
    throw IllegalStateException("Could not sign in with Google, missing serverClientId.")

private fun failWithMissingNonce(): Nothing =
    throw IllegalStateException("Could not sign in with Google, missing nonce.")

private fun failWithSignInFailure(cause: Throwable? = null): Nothing {
    val details = cause?.let { ", caused by: $cause" } ?: "."
    throw OAuthException("Could not sign in with Google$details")
}