package com.jqwave.data

/**
 * Stable IDs so DB migration and fresh seed match.
 */
object DefaultEventRules {

    private const val SHABBAT_START_ID = "00000000-0000-4000-8000-000000000001"
    private const val SHABBAT_END_ID = "00000000-0000-4000-8000-000000000002"

    val shabbatRules: List<NotificationRule> = listOf(
        NotificationRule(
            id = SHABBAT_START_ID,
            anchor = TimeAnchor.SUNSET,
            hour = 9,
            minute = 0,
            offsetMinutes = -18,
            shabbatSegment = ShabbatSegment.START,
        ),
        NotificationRule(
            id = SHABBAT_END_ID,
            anchor = TimeAnchor.TZAIT,
            hour = 9,
            minute = 0,
            offsetMinutes = 0,
            shabbatSegment = ShabbatSegment.END,
        ),
    )

    fun shabbatRulesJson(): String = shabbatRules.toJson()
}
