package com.jqwave.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ShabbatSegment {
    @SerialName("START")
    START,

    @SerialName("END")
    END,
}
