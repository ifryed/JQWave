package com.jqwave

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jqwave.locale.AppLocaleStore
import com.jqwave.location.LocationRefreshScheduler
import com.jqwave.ui.EventListScreen
import com.jqwave.ui.EventsViewModel
import com.jqwave.ui.SettingsScreen
import com.jqwave.ui.EventsViewModelFactory
import com.jqwave.ui.theme.JQWaveTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val cfg = Configuration(newBase.resources.configuration)
        cfg.setLocale(AppLocaleStore.localeForTag(AppLocaleStore.getTag(newBase)))
        cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        super.attachBaseContext(newBase.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JQWaveTheme {
                val vm: EventsViewModel = viewModel(
                    factory = EventsViewModelFactory(application),
                )
                val rows by vm.eventRows.collectAsStateWithLifecycle()
                val location by vm.location.collectAsStateWithLifecycle()

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { _: Boolean -> }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { grants ->
                    if (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                        LocationRefreshScheduler.requestImmediateRefresh(this@MainActivity)
                    }
                }

                val refreshLocationFromDevice: () -> Unit = {
                    val coarseGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (coarseGranted) {
                        LocationRefreshScheduler.requestImmediateRefresh(this@MainActivity)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    }
                }

                var showSettings by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val coarseGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!coarseGranted) {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    }
                }

                if (showSettings) {
                    SettingsScreen(
                        location = location,
                        onBack = { showSettings = false },
                        onUpdateLocationFromDevice = refreshLocationFromDevice,
                        onCityChosen = { label, lat, lon, tz ->
                            vm.updateLocation(lat, lon, tz, label)
                        },
                        onInIsraelChange = vm::setInIsrael,
                    )
                } else {
                    EventListScreen(
                        rows = rows,
                        onEnabledChange = vm::setEnabled,
                        onRulesChange = vm::saveRules,
                        onTestEventNotification = vm::testEventNotification,
                        onLanguageToggle = {
                            val next = if (AppLocaleStore.getTag(this@MainActivity) == AppLocaleStore.LANG_IW) {
                                AppLocaleStore.LANG_EN
                            } else {
                                AppLocaleStore.LANG_IW
                            }
                            AppLocaleStore.setTag(this@MainActivity, next)
                            recreate()
                        },
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}
