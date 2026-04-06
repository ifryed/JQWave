package com.jqwave.data

import androidx.annotation.StringRes
import com.jqwave.R

enum class EventKind(
    val storageKey: String,
    @StringRes val displayNameRes: Int,
    @StringRes val notificationTitleRes: Int,
) {
    ROSH_HODESH(
        "ROSH_HODESH",
        R.string.event_name_rosh_hodesh,
        R.string.notify_title_rosh_hodesh,
    ),
    SFIRAT_HAOMER(
        "SFIRAT_HAOMER",
        R.string.event_name_sfirat_haomer,
        R.string.notify_title_sfirat_haomer,
    ),
    SHABBAT(
        "SHABBAT",
        R.string.event_name_shabbat,
        R.string.notify_title_shabbat_card,
    ),
}
