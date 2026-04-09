package com.jqwave.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

data class GeocodedCity(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
)

object GeocodingHelper {

    suspend fun searchCities(context: Context, query: String, maxResults: Int = 10): List<GeocodedCity> =
        withContext(Dispatchers.IO) {
            if (query.isBlank() || !Geocoder.isPresent()) return@withContext emptyList()
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, maxResults) ?: return@withContext emptyList()
            addresses.mapNotNull { addr ->
                val lat = addr.latitude
                val lon = addr.longitude
                if (!lat.isFinite() || !lon.isFinite()) return@mapNotNull null
                val label = formatAddressLabel(addr) ?: return@mapNotNull null
                GeocodedCity(
                    label = label,
                    latitude = lat,
                    longitude = lon,
                    timeZoneId = timeZoneIdForGeocodedAddress(addr, lat, lon),
                )
            }.distinctBy { it.label to (it.latitude to it.longitude) }
        }

    suspend fun reverseGeocodeLabel(context: Context, latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(latitude, longitude, 1) ?: return@withContext null
            list.firstOrNull()?.let { formatAddressLabel(it) }
        }

    private fun formatAddressLabel(a: Address): String? {
        val parts = buildList {
            a.locality?.takeIf { it.isNotBlank() }?.let { add(it) }
            a.adminArea?.takeIf { it.isNotBlank() }?.let { add(it) }
            a.countryName?.takeIf { it.isNotBlank() }?.let { add(it) }
        }.distinct()
        if (parts.isEmpty()) {
            a.featureName?.takeIf { it.isNotBlank() }?.let { return it }
            return null
        }
        return parts.joinToString(", ")
    }

    /**
     * Geocoder does not return IANA zones; infer a reasonable ID from country and rough longitude.
     */
    fun timeZoneIdForGeocodedAddress(address: Address, lat: Double, lon: Double): String {
        val cc = address.countryCode?.uppercase(Locale.US) ?: return TimeZone.getDefault().id
        if (cc == "IL") return "Asia/Jerusalem"
        if (cc == "US") {
            return when {
                lon < -115.0 -> "America/Los_Angeles"
                lon < -102.0 -> "America/Denver"
                lon < -87.0 -> "America/Chicago"
                else -> "America/New_York"
            }
        }
        if (cc == "CA") {
            return when {
                lat > 55.0 -> "America/Whitehorse"
                lon < -110.0 -> "America/Edmonton"
                else -> "America/Toronto"
            }
        }
        if (cc == "AU") {
            return if (lon < 125.0) "Australia/Perth" else "Australia/Sydney"
        }
        if (cc == "GB") return "Europe/London"
        if (cc == "FR") return "Europe/Paris"
        if (cc == "DE") return "Europe/Berlin"
        if (cc == "ES") return "Europe/Madrid"
        if (cc == "IT") return "Europe/Rome"
        if (cc == "BR") return "America/Sao_Paulo"
        if (cc == "MX") return "America/Mexico_City"
        if (cc == "ZA") return "Africa/Johannesburg"
        if (cc == "RU") {
            return when {
                lon > 100.0 -> "Asia/Vladivostok"
                lon > 40.0 -> "Asia/Yekaterinburg"
                else -> "Europe/Moscow"
            }
        }
        if (cc == "AR") return "America/Argentina/Buenos_Aires"
        if (cc == "CL") return "America/Santiago"
        if (cc == "SG") return "Asia/Singapore"
        if (cc == "IN") return "Asia/Kolkata"
        if (cc == "CN") return "Asia/Shanghai"
        if (cc == "JP") return "Asia/Tokyo"
        return TimeZone.getDefault().id
    }
}
