package com.jqwave.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TimeAnchor {
    @SerialName("CLOCK")
    CLOCK,

    @SerialName("SUNRISE")
    SUNRISE,

    @SerialName("SUNSET")
    SUNSET,
}
