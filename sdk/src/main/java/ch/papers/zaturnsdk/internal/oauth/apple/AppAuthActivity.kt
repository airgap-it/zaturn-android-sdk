package ch.papers.zaturnsdk.internal.oauth.apple

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import ch.papers.zaturnsdk.internal.oauth.google.AppAuthActivity
import ch.papers.zaturnsdk.internal.util.addNotNull
import ch.papers.zaturnsdk.internal.util.catch
import ch.papers.zaturnsdk.internal.util.tryComplete
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch
import net.openid.appauth.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class AppAuthActivity : AppCompatActivity() {
    private var idTokenDeferred: CompletableDeferred<String?>? = null

    private val disposableHandles: MutableList<DisposableHandle> = mutableListOf()

    private val authorizationService: AuthorizationService by lazy { AuthorizationService(this) }

    private val authorizationEndpoint: Uri
        get() = Uri.parse(AUTHORIZATION_ENDPOINT)

    private val tokenEndpoint: Uri
        get() = Uri.parse(TOKEN_ENDPOINT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientId = intent.extras?.getString(EXTRA_CLIENT_ID) ?: failWithMissingClientId()
        val redirectUri = intent.extras?.getString(EXTRA_REDIRECT_URI) ?: failWithMissingRedirectUri()
        val nonce = intent.extras?.getString(EXTRA_NONCE) ?: failWithMissingNonce()

        idTokenDeferred = AppleOAuth.instance().idTokenDeferred(clientId)

        disposableHandles.addNotNull(idTokenDeferred?.invokeOnCompletion { finish() })
        idTokenDeferred?.catch {
            appAuthSignIn(clientId, redirectUri, nonce)
        }
    }

    override fun onDestroy() {
        disposableHandles.forEach { it.dispose() }
        super.onDestroy()
    }

    private fun appAuthSignIn(clientId: String, redirectUri: String, nonce: String) {
        val configuration = AuthorizationServiceConfiguration(authorizationEndpoint, tokenEndpoint)
        val authorizationRequest = AuthorizationRequest.Builder(
            configuration,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri),
        ).apply {
            setScope("${AuthorizationRequest.Scope.OPENID} ${AuthorizationRequest.Scope.EMAIL}")
            setNonce(nonce)
            setResponseMode("form_post")
        }.build()

        authorize(authorizationRequest)
    }

    private fun authorize(request: AuthorizationRequest) {
        val intent = authorizationService.getAuthorizationRequestIntent(request)

        val authorize = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            idTokenDeferred?.tryComplete {
                when (it.resultCode) {
                    Activity.RESULT_OK -> {
                        val data = it.data ?: failWithSignInFailure()

                        val response = AuthorizationResponse.fromIntent(data)
                        val exception = AuthorizationException.fromIntent(data)

                        if (exception != null || response == null) failWithSignInFailure(exception)

                        response.idToken
                    }
                    else -> failWithSignInFailure()
                }
            }
        }

        authorize.launch(intent)
    }

    companion object {
        const val EXTRA_CLIENT_ID = "clientId"
        const val EXTRA_REDIRECT_URI = "redirectUri"
        const val EXTRA_NONCE = "nonce"

        private const val AUTHORIZATION_ENDPOINT = "https://appleid.apple.com/auth/authorize"
        private const val TOKEN_ENDPOINT = ""
    }
}

private fun failWithMissingClientId(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing clientId.")

private fun failWithMissingRedirectUri(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing redirectUri.")

private fun failWithMissingNonce(): Nothing =
    throw IllegalStateException("Could not sign in with Apple, missing nonce.")

private fun failWithSignInFailure(cause: Throwable? = null): Nothing {
    val details = cause?.let { ", caused by: $cause" } ?: "."
    throw OAuthException("Could not sign in with Apple$details")
}