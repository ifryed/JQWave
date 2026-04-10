package com.jqwave.domain

import android.content.Context
import com.jqwave.R
import com.jqwave.data.OmerNusach

object OmerLiturgy {

    fun notificationBody(context: Context, dayOfOmer: Int, nusach: OmerNusach): String {
        if (dayOfOmer < 1) {
            return context.getString(R.string.notify_body_omer)
        }
        val bracha = bracha(context, nusach)
        val useBa = nusach == OmerNusach.SEPHARADI
        val count = OmerCountTable.line(dayOfOmer, useBa)
        return "$bracha\n\n$count"
    }

    /** Blessing and day count are always Hebrew (לעומר vs בעומר follows nusach). */
    private fun bracha(context: Context, nusach: OmerNusach): String =
        when (nusach) {
            OmerNusach.YEMENITE -> context.getString(R.string.omer_bracha_yemenite_he)
            OmerNusach.ASHKENAZI, OmerNusach.SEPHARADI ->
                context.getString(R.string.omer_bracha_standard_he)
        }
}
