package ch.papers.zaturnsdk.internal.network.http.ktor

import android.util.Log
import ch.papers.zaturnsdk.BuildConfig
import ch.papers.zaturnsdk.internal.network.http.Http
import ch.papers.zaturnsdk.internal.network.http.data.HttpHeader
import ch.papers.zaturnsdk.internal.network.http.data.HttpParameter
import ch.papers.zaturnsdk.internal.util.decodeFromString
import ch.papers.zaturnsdk.internal.util.url
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class KtorHttp(baseUrl: String) : Http(baseUrl) {
    private val ktorClient by lazy {
        HttpClient(OkHttp) {
            defaultRequest {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        if (BuildConfig.DEBUG) Log.d(TAG, message)
                    }
                }
                level = LogLevel.ALL
            }
        }
    }

    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun <T : Any> get(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        responseClass: KClass<T>,
    ): T = request(HttpMethod.Get, endpoint, headers, parameters, responseClass)

    override suspend fun <T : Any, R : Any> post(
        endpoint: String,
        requestBody: T?,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
    ): R = request(HttpMethod.Post, endpoint, headers, parameters, responseClass) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        requestBody?.let { body = it }
    }

    override suspend fun <T : Any> head(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        responseClass: KClass<T>
    ): T = request(HttpMethod.Head, endpoint, headers, parameters, responseClass)

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <T : Any> request(
        method: HttpMethod,
        endpoint: String,
        httpHeaders: List<HttpHeader>,
        httpParameters: List<HttpParameter>,
        responseClass: KClass<T>,
        block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = ktorClient.request<String> {
            this.method = method
            url(apiUrl(endpoint))
            headers(httpHeaders)
            parameters(httpParameters)
            block(this)
        }

        return if (responseClass != Unit::class) json.decodeFromString(
            response,
            responseClass
        ) else Unit as T
    }

    private fun apiUrl(endpoint: String): String = url(baseUrl, endpoint)

    private fun HttpRequestBuilder.headers(headers: List<HttpHeader>) {
        headers.forEach { header(it.first, it.second) }
    }

    private fun HttpRequestBuilder.parameters(parameters: List<HttpParameter>) {
        parameters.forEach { parameter(it.first, it.second) }
    }

    companion object {
        private const val TAG = "Http"
    }
}