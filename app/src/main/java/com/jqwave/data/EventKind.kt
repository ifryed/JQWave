package com.jqwave.data

enum class EventKind(val storageKey: String, val displayName: String, val notificationTitle: String) {
    ROSH_HODESH(
        "ROSH_HODESH",
        "Rosh Chodesh",
        "Rosh Chodesh today"
    ),
    SFIRAT_HAOMER(
        "SFIRAT_HAOMER",
        "Sfirat HaOmer",
        "Sfirat HaOmer — count the Omer"
    ),
}
