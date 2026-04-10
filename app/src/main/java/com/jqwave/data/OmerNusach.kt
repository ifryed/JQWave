package com.jqwave.data

enum class OmerNusach(val storageKey: String) {
    ASHKENAZI("ASHKENAZI"),
    SEPHARADI("SEPHARADI"),
    YEMENITE("YEMENITE"),
    ;

    companion object {
        fun fromStorageKey(key: String?): OmerNusach =
            entries.find { it.storageKey == key } ?: ASHKENAZI
    }
}
