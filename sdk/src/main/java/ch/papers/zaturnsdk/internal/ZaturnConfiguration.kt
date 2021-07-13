package ch.papers.zaturnsdk.internal

import ch.papers.zaturnsdk.BuildConfig

internal object ZaturnConfiguration {
    const val VERSION = 1

    const val API = "/api/v1"

    const val MIN_GROUPS = 1L
    const val MIN_GROUP_THRESHOLD = 1L
    const val MIN_GROUP_MEMBERS = 2L
    const val MIN_GROUP_MEMBER_THRESHOLD = 2L
}