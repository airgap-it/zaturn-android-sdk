package ch.papers.zaturnsdk.internal.oauth.google

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.papers.zaturnsdk.internal.oauth.exception.OAuthException
import ch.papers.zaturnsdk.internal.util.addNotNull
import ch.papers.zaturnsdk.internal.util.catch
import ch.papers.zaturnsdk.internal.util.tryComplete
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInResult
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class GoogleSignInActivity : AppCompatActivity() {
    private var idTokenDeferred: CompletableDeferred<String?>? = null

    private val disposableHandles: MutableList<DisposableHandle> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serverClientId = intent.extras?.getString(EXTRA_SERVER_CLIENT_ID) ?: failWithMissingServerClientId()
        val scopes = intent.extras?.getStringArrayList(EXTRA_SCOPES) ?: emptyList<String>()
        val nonce = intent.extras?.getString(EXTRA_NONCE) ?: failWithMissingNonce()

        idTokenDeferred = GoogleOAuth.instance().idTokenDeferred(serverClientId)

        disposableHandles.addNotNull(idTokenDeferred?.invokeOnCompletion { finish() })
        lifecycleScope.launch {
            idTokenDeferred?.catch {
                oneTapSignIn(serverClientId, nonce)
            }
        }
    }

    override fun onDestroy() {
        disposableHandles.forEach { it.dispose() }
        super.onDestroy()
    }

    private suspend fun oneTapSignIn(serverClientId: String, nonce: String) {
        val oneTapClient = Identity.getSignInClient(this)

        val signInRequest = BeginSignInRequest.builder().apply {
            val tokenRequestOptions =
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder().apply {
                    setSupported(true)
                    setServerClientId(serverClientId)
                    setNonce(nonce)
                    setFilterByAuthorizedAccounts(true)
                }.build()
            setGoogleIdTokenRequestOptions(tokenRequestOptions)

            setAutoSelectEnabled(true)
        }.build()

        val startIntentSenderForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            idTokenDeferred?.tryComplete {
                when (it.resultCode) {
                    Activity.RESULT_OK -> oneTapClient.getSignInCredentialFromIntent(it.data)?.googleIdToken
                    else -> failWithSignInFailure()
                }
            }
        }

        val signInResult = suspendCoroutine<BeginSignInResult?> { continuation ->
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { continuation.resume(it) }
                .addOnFailureListener(this) { continuation.resume(null) }
        } ?: failWithNoAccounts()

        val intentSenderRequest = IntentSenderRequest.Builder(signInResult.pendingIntent).build()
        startIntentSenderForResult.launch(intentSenderRequest)
    }

    companion object {
        const val EXTRA_SERVER_CLIENT_ID = "serverClientId"
        const val EXTRA_SCOPES = "scopes"
        const val EXTRA_NONCE = "nonce"
    }
}

private fun failWithMissingServerClientId(): Nothing =
    throw IllegalStateException("Could not sign in with Google, missing serverClientId.")

private fun failWithMissingNonce(): Nothing =
    throw IllegalStateException("Could not sign in with Google, missing nonce.")

private fun failWithNoAccounts(): Nothing =
    throw OAuthException("Could not sign in with Google, no Google Accounts found.")

private fun failWithSignInFailure(): Nothing =
    throw OAuthException("Could not sign in with Google.")
