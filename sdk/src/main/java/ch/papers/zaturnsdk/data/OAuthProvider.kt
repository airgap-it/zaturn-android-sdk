package ch.papers.zaturnsdk.data

public sealed interface OAuthProvider {
    public object Apple : OAuthProvider
    public data class Google(val serverClientId: String) : OAuthProvider
}