package ch.papers.zaturnsdk.data

public data class OAuthId(
    val accessToken: String? = null,
    val expiresIn: Long? = null,
    val idToken: String,
    val scope: String? = null,
    val tokenType: String? = null,
    val refreshToken: String? = null,
    val additional: Map<String, String> = emptyMap(),
) {
    public companion object {}
}
