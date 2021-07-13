package ch.papers.zaturnsdk.internal.exception

internal abstract class ZaturnInternalException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    abstract val module: String

    override fun toString(): String = "[$module] ${super.toString()}"
}