package ch.papers.zaturnsdk.internal.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

internal fun <T : Any> Json.encodeToString(value: T, sourceClass: KClass<T>): String =
    encodeToString(serializerFor(sourceClass), value)

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Json.decodeFromString(string: String, targetClass: KClass<T>): T =
    decodeFromString(serializerFor(targetClass), string) as T

private fun serializerFor(targetClass: KClass<out Any>): KSerializer<Any?> {
    val type = targetClass.createType()
    return serializer(type)
}