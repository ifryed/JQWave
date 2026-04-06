package com.jqwave.location

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object LocationRefreshScheduler {

    private const val PERIODIC_NAME = "location_refresh_24h"
    private const val ONE_SHOT_NAME = "location_refresh_now"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<LocationUpdateWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun requestImmediateRefresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<LocationUpdateWorker>()
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_SHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
