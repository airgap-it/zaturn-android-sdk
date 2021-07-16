package ch.papers.zaturnsdk.data

public sealed interface OAuthProvider {
    public data class Apple(val clientId: String, val redirectUri: String) : OAuthProvider
    public data class Google(val clientId: String, val serverClientId: String) : OAuthProvider
}