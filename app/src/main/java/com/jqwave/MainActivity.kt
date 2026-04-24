package com.jqwave

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
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
import com.jqwave.data.EventKind
import com.jqwave.data.resolveStoredNotificationSound
import com.jqwave.locale.AppLocaleStore
import com.jqwave.location.LocationRefreshScheduler
import com.jqwave.ui.EventListScreen
import com.jqwave.ui.EventsViewModel
import com.jqwave.ui.EventsViewModelFactory
import com.jqwave.ui.RingtonePickerHelper
import com.jqwave.ui.SettingsScreen
import com.jqwave.ui.theme.JQWaveTheme

private sealed interface PendingNotificationSoundPick {
    data object AppDefault : PendingNotificationSoundPick
    data class Rule(val kind: EventKind, val ruleId: String) : PendingNotificationSoundPick
}

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
                val omerNusach by vm.omerNusach.collectAsStateWithLifecycle()
                val defaultNotificationSoundStored by vm.defaultNotificationSoundStored.collectAsStateWithLifecycle()

                var pendingSoundPick by remember { mutableStateOf<PendingNotificationSoundPick?>(null) }
                val ringtonePickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    RingtonePickerHelper.takePersistableReadIfNeeded(this@MainActivity, result)
                    val target = pendingSoundPick ?: return@rememberLauncherForActivityResult
                    pendingSoundPick = null
                    val stored = RingtonePickerHelper.parsePickResult(result)
                        ?: return@rememberLauncherForActivityResult
                    when (target) {
                        PendingNotificationSoundPick.AppDefault ->
                            vm.setDefaultNotificationSoundStored(stored)
                        is PendingNotificationSoundPick.Rule ->
                            vm.patchRuleNotificationSoundOverride(target.kind, target.ruleId, stored)
                    }
                }

                val openAudioDocumentLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri: Uri? ->
                    val target = pendingSoundPick
                    pendingSoundPick = null
                    if (uri == null || target == null) return@rememberLauncherForActivityResult
                    RingtonePickerHelper.takePersistableReadForPickedDocument(this@MainActivity, uri)
                    val stored = uri.toString()
                    when (target) {
                        PendingNotificationSoundPick.AppDefault ->
                            vm.setDefaultNotificationSoundStored(stored)
                        is PendingNotificationSoundPick.Rule ->
                            vm.patchRuleNotificationSoundOverride(target.kind, target.ruleId, stored)
                    }
                }

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
                        omerNusach = omerNusach,
                        defaultNotificationSoundStored = defaultNotificationSoundStored,
                        onOmerNusachChange = vm::setOmerNusach,
                        onBack = { showSettings = false },
                        onUpdateLocationFromDevice = refreshLocationFromDevice,
                        onCityChosen = { label, lat, lon, tz ->
                            vm.updateLocation(lat, lon, tz, label)
                        },
                        onInIsraelChange = vm::setInIsrael,
                        onPickDefaultNotificationSound = {
                            val existing = resolveStoredNotificationSound(
                                this@MainActivity,
                                defaultNotificationSoundStored,
                            )
                            pendingSoundPick = PendingNotificationSoundPick.AppDefault
                            ringtonePickerLauncher.launch(
                                RingtonePickerHelper.buildNotificationPickIntent(existing),
                            )
                        },
                        onResetDefaultNotificationSound = {
                            vm.setDefaultNotificationSoundStored(null)
                        },
                        onPickDefaultNotificationAudioFile = {
                            pendingSoundPick = PendingNotificationSoundPick.AppDefault
                            openAudioDocumentLauncher.launch(
                                RingtonePickerHelper.notificationAudioOpenDocumentMimeTypes,
                            )
                        },
                    )
                } else {
                    EventListScreen(
                        rows = rows,
                        defaultNotificationSoundStored = defaultNotificationSoundStored,
                        onEnabledChange = vm::setEnabled,
                        onRulesChange = vm::saveRules,
                        onTestEventNotification = { kind, soundRule ->
                            vm.testEventNotification(kind, soundRule)
                        },
                        onSetRuleUseAppNotificationSound = vm::setRuleUseAppNotificationSound,
                        onPickRuleNotificationSound = { kind, ruleId, existing ->
                            pendingSoundPick = PendingNotificationSoundPick.Rule(kind, ruleId)
                            ringtonePickerLauncher.launch(
                                RingtonePickerHelper.buildNotificationPickIntent(existing),
                            )
                        },
                        onPickRuleNotificationAudioFile = { kind, ruleId ->
                            pendingSoundPick = PendingNotificationSoundPick.Rule(kind, ruleId)
                            openAudioDocumentLauncher.launch(
                                RingtonePickerHelper.notificationAudioOpenDocumentMimeTypes,
                            )
                        },
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
