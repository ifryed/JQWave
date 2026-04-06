package com.jqwave.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_configs")
data class EventConfigEntity(
    @PrimaryKey val kind: String,
    val enabled: Boolean,
    val rulesJson: String,
)
