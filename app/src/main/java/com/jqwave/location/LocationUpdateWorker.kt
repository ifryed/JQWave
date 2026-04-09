package com.jqwave.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.jqwave.JQWaveApplication
import com.jqwave.R
import kotlinx.coroutines.tasks.await
import java.util.TimeZone

class LocationUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val app = ctx.applicationContext as? JQWaveApplication ?: return Result.failure()

        val client = LocationServices.getFusedLocationProviderClient(ctx)
        val loc = try {
            client.lastLocation.await()
                ?: client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
        } catch (_: Exception) {
            null
        } ?: return Result.retry()

        val tz = TimeZone.getDefault().id
        val label = GeocodingHelper.reverseGeocodeLabel(ctx, loc.latitude, loc.longitude)
            ?: ctx.getString(R.string.location_label_gps_fallback)
        app.locationPreferences.update(loc.latitude, loc.longitude, tz, label)
        app.eventNotificationScheduler.rescheduleAll()
        return Result.success()
    }
}
