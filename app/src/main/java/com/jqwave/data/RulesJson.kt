package com.jqwave.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val listSerializer = ListSerializer(NotificationRule.serializer())

fun List<NotificationRule>.toJson(): String = json.encodeToString(listSerializer, this)

fun String.toNotificationRules(): List<NotificationRule> =
    json.decodeFromString(listSerializer, this)
