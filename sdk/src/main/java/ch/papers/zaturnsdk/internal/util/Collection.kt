package ch.papers.zaturnsdk.internal.util

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal fun <T> MutableList<T>.addNotNull(element: T?): Boolean = if (element != null) add(element) else false

internal suspend fun <T> List<T>.launch(block: suspend (T) -> Unit) {
    coroutineScope {
        forEach {
            launch { block(it) }
        }
    }
}

internal suspend fun <T> List<T>.launchIndexed(block: suspend (index: Int, T) -> Unit) {
    coroutineScope {
        forEachIndexed { index, t ->
            launch { block(index, t) }
        }
    }
}

internal suspend fun <T, R> List<T>.async(block: suspend (T) -> R): List<R> =
    coroutineScope {
        map {
            async { block(it) }
        }
    }.awaitAll()

internal suspend fun <T, R> List<T>.asyncIndexed(block: suspend (index: Int, T) -> R): List<R> =
    coroutineScope {
        mapIndexed { index, t ->
            async { block(index, t) }
        }
    }.awaitAll()
