package ch.papers.zaturnsdk.internal.network.http

import ch.papers.zaturnsdk.internal.network.http.data.HttpHeader
import ch.papers.zaturnsdk.internal.network.http.data.HttpParameter
import kotlin.reflect.KClass

internal abstract class Http(protected val baseUrl: String) {
    suspend inline fun <reified T : Any> get(
        endpoint: String,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
    ): T = get(endpoint, headers, parameters, T::class)

    suspend inline fun <reified T : Any, reified R : Any> post(
        endpoint: String,
        body: T? = null,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
    ): R = post(endpoint, body, headers, parameters, T::class, R::class)

    protected abstract suspend fun <T : Any> get(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        responseClass: KClass<T>,
    ): T

    protected abstract suspend fun <T : Any, R : Any> post(
        endpoint: String,
        requestBody: T?,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
    ): R
}