package ch.papers.zaturnsdk.internal.util

internal fun url(base: String, path: String): String {
    val base = base.trimEnd('/')
    val path = path.trimStart('/')

    return "$base/$path"
}