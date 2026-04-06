package com.jqwave.domain

import com.kosherjava.zmanim.ComplexZmanimCalendar
import java.util.Date

internal val ComplexZmanimCalendar.sunrise: Date?
    get() = getSunrise()

internal val ComplexZmanimCalendar.sunset: Date?
    get() = getSunset()

internal val ComplexZmanimCalendar.tzait7083: Date?
    get() = getTzaisGeonim7Point083Degrees()
