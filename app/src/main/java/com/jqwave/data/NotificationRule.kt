package com.jqwave.data

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class NotificationRule(
    val id: String = UUID.randomUUID().toString(),
    val anchor: TimeAnchor = TimeAnchor.CLOCK,
    /** Local wall clock hour (0–23) when [anchor] is [TimeAnchor.CLOCK]. */
    val hour: Int = 9,
    val minute: Int = 0,
    /** Minutes after sunrise or before/after sunset when anchor is sun-based (negative = before). */
    val offsetMinutes: Int = 0,
)
