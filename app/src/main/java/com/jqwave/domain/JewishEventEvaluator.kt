package com.jqwave.domain

import com.jqwave.data.EventKind
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar

object JewishEventEvaluator {

    fun applies(kind: EventKind, jc: JewishCalendar): Boolean = when (kind) {
        EventKind.ROSH_HODESH -> jc.isRoshChodesh
        EventKind.SFIRAT_HAOMER -> jc.getDayOfOmer() >= 1
    }
}
