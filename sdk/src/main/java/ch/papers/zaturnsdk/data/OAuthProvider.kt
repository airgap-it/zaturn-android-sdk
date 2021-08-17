package ch.papers.zaturnsdk.data

public sealed interface OAuthProvider {

    public data class Apple(
        val clientId: String,
        val serverClientId: String,
        val redirectUri: String,
        val responseTypes: List<String>,
        val responseMode: String,
        val scopes: List<String> = emptyList(),
    ) : OAuthProvider {
        public companion object {}
    }

    public data class Google(
        val clientId: String,
        val serverClientId: String,
        val scopes: List<String> = emptyList(),
    ) : OAuthProvider {
        public companion object {}
    }

}