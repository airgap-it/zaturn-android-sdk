package ch.papers.zaturnsdk.internal.util

import kotlinx.coroutines.CompletableDeferred

internal inline fun <T> CompletableDeferred<T>.tryComplete(block: () -> T) {
    try {
        complete(block())
    } catch (e: Exception) {
        completeExceptionally(e)
    }
}

internal inline fun <T> CompletableDeferred<T>.catch(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        completeExceptionally(e)
    }
}