package ch.papers.zaturnsdk.internal.util

import android.util.Base64

internal fun ByteArray.encodeToBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
internal fun String.decodeFromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)