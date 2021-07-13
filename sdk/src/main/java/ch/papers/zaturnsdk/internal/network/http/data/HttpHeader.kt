package ch.papers.zaturnsdk.internal.network.http.data

internal typealias HttpHeader = Pair<String, String>

internal fun Token(token: String): HttpHeader = HttpHeader("Authorization", token)