package com.prenatal.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.CalendarContract
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.prenatal.app.data.Attachment
import com.prenatal.app.data.Event
import com.prenatal.app.data.Measurement
import com.prenatal.app.data.Protocol
import com.prenatal.app.data.ProtocolItem
import com.prenatal.app.data.Result
import com.prenatal.app.ui.AttachmentViewer
import com.prenatal.app.ui.BagScreen
import com.prenatal.app.ui.BusyOverlay
import com.prenatal.app.ui.EraseDialog
import com.prenatal.app.ui.EventEditScreen
import com.prenatal.app.ui.HealthScreen
import com.prenatal.app.ui.HomeScreen
import com.prenatal.app.ui.LockScreen
import com.prenatal.app.ui.MeasurementEditScreen
import com.prenatal.app.ui.MoreScreen
import com.prenatal.app.ui.P
import com.prenatal.app.ui.PassphraseDialog
import com.prenatal.app.ui.PrenatalTheme
import com.prenatal.app.ui.ProfileEditScreen
import com.prenatal.app.ui.ProtocolDialog
import com.prenatal.app.ui.ResultEditScreen
import com.prenatal.app.ui.ResultViewScreen
import com.prenatal.app.ui.ResultsScreen
import com.prenatal.app.ui.ScanChooser
import com.prenatal.app.ui.ScheduleScreen
import com.prenatal.app.ui.SetupScreen
import com.prenatal.app.ui.draftEvent
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var stoppedAt = 0L
    private var authError by mutableStateOf<String?>(null)
    private var promptShowing = false
    private val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep medical records out of the recent-apps preview and screen recordings.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            PrenatalTheme {
                AppRoot(
                    vm = vm,
                    canAuthenticate = canAuthenticate(),
                    authError = authError,
                    onUnlock = ::authenticate,
                    onOpenSecuritySettings = { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                    onShare = ::share,
                    onAddToCalendar = ::addToCalendar,
                    needsNotificationPermission = {
                        Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (vm.externalActivity) {
            vm.externalActivity = false
        } else if (vm.unlocked && stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt >= vm.lockAfterSeconds * 1000L) {
            vm.lock()
        }
        stoppedAt = 0
    }

    override fun onResume() {
        super.onResume()
        if (!vm.unlocked && canAuthenticate()) authenticate()
    }

    override fun onStop() {
        super.onStop()
        if (!vm.externalActivity && !isChangingConfigurations) stoppedAt = SystemClock.elapsedRealtime()
    }

    private fun canAuthenticate() =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    private fun authenticate() {
        if (promptShowing || vm.unlocked || vm.busy != null) return
        promptShowing = true
        authError = null
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptShowing = false
                    vm.onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptShowing = false
                    val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    if (!cancelled) authError = errString.toString()
                }
            },
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Prenatal")
                .setSubtitle("Confirm it’s you to open your records")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    /** Opens the phone's calendar with the appointment filled in, ready to save there too. */
    private fun addToCalendar(e: Event) {
        val start = e.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 60 * 60 * 1000L)
            .putExtra(CalendarContract.Events.TITLE, e.title)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, e.place)
            .putExtra(CalendarContract.Events.DESCRIPTION, listOf(e.professional, e.notes).filter { it.isNotBlank() }.joinToString("\n"))
        vm.externalActivity = true
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            vm.externalActivity = false
            vm.say("No calendar app found on this phone")
        }
    }

    private fun share(uri: Uri) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        vm.externalActivity = true
        startActivity(Intent.createChooser(send, "Share"))
    }
}

@Composable
private fun AppRoot(
    vm: AppViewModel,
    canAuthenticate: Boolean,
    authError: String?,
    onUnlock: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onShare: (Uri) -> Unit,
    onAddToCalendar: (Event) -> Unit,
    needsNotificationPermission: () -> Boolean,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    var protocolOpen by remember { mutableStateOf<ProtocolItem?>(null) }
    var askExport by remember { mutableStateOf(false) }
    var exportPass by remember { mutableStateOf<String?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var askErase by remember { mutableStateOf(false) }
    val newAttachments = remember { mutableStateListOf<Attachment>() }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var scanChooser by remember { mutableStateOf(false) }
    var scanForResult by remember { mutableStateOf(true) }
    var scanCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun scanPicked(uri: Uri?) {
        if (uri == null) return
        if (scanForResult) vm.scanResult(uri) { newAttachments.add(it) } else vm.scanAppointment(uri)
    }

    fun addPicked(uri: Uri?) {
        if (uri == null) return
        scope.launch { vm.readPicked(uri)?.let { newAttachments.add(it) } }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { addPicked(it) }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { addPicked(it) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) addPicked(cameraUri) }
    val scanCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) scanPicked(scanCameraUri) }
    val scanGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { scanPicked(it) }

    /** Photograph or pick a document to read; [forResult] picks how it's interpreted. */
    fun startScan(forResult: Boolean, camera: Boolean) {
        scanForResult = forResult
        vm.externalActivity = true
        if (camera) {
            val uri = vm.newCameraUri()
            scanCameraUri = uri
            scanCamera.launch(uri)
        } else {
            scanGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun openNewResult() {
        newAttachments.clear()
        vm.clearScans()
        vm.open(Screen.EditResult(null))
    }

    fun openEvent(id: String?, protocolKey: String? = null) {
        vm.clearScans()
        vm.open(Screen.EditEvent(id, protocolKey))
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.say("Reminders are off. You can allow notifications in the phone’s settings.")
    }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pass = exportPass
        exportPass = null
        if (uri != null && pass != null) vm.exportBackup(uri, pass)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it }

    fun saveEvent(e: Event) {
        vm.saveEvent(e)
        vm.back()
        if (e.remind && !e.done && needsNotificationPermission()) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    BackHandler(enabled = vm.unlocked && vm.stack.size > 1) { vm.back() }
    BackHandler(enabled = vm.unlocked && vm.stack.size == 1 && vm.tab != Tab.HOME) { vm.tab = Tab.HOME }

    Box(Modifier.fillMaxSize().background(P.Bg)) {
        val data = vm.data
        val g = vm.gestation
        when {
            !vm.unlocked || data == null -> LockScreen(canAuthenticate, authError, onUnlock, onOpenSecuritySettings)
            data.profile.dueDate == null -> SetupScreen { due, mother, baby -> vm.setDueDate(due, mother, baby) }
            else -> AnimatedContent(
                targetState = vm.screen,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                label = "screens",
            ) { s ->
                val now = LocalDateTime.now()
                when (s) {
                    Screen.Tabs -> Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (vm.tab) {
                                Tab.HOME -> HomeScreen(
                                    g!!, data.profile.motherName, data.profile.babyName, data.events, data.skippedProtocol, now,
                                    onOpenEvent = { openEvent(it) },
                                    onOpenProtocol = { protocolOpen = it },
                                    onAddEvent = { openEvent(null) },
                                    onAddResult = { openNewResult() },
                                    onAddMeasurement = { vm.open(Screen.EditMeasurement(null)) },
                                    onSeeSchedule = { vm.tab = Tab.SCHEDULE },
                                    onScan = { scanChooser = true },
                                )
                                Tab.SCHEDULE -> ScheduleScreen(
                                    g, data.events, data.skippedProtocol, now,
                                    onOpenEvent = { openEvent(it) },
                                    onOpenProtocol = { protocolOpen = it },
                                    onAdd = { openEvent(null) },
                                )
                                Tab.RESULTS -> ResultsScreen(
                                    data.results, g?.dueDate, { vm.thumbnail(it) },
                                    onOpen = { vm.open(Screen.ViewResult(it)) },
                                    onAdd = { openNewResult() },
                                )
                                Tab.HEALTH -> HealthScreen(
                                    data.measurements, g?.dueDate,
                                    onAdd = { vm.open(Screen.EditMeasurement(null)) },
                                    onOpen = { vm.open(Screen.EditMeasurement(it)) },
                                )
                                Tab.MORE -> MoreScreen(
                                    data.profile, data.bag, vm.lockAfterSeconds, BuildConfig.VERSION_NAME,
                                    onEditProfile = { vm.open(Screen.EditProfile) },
                                    onOpenBag = { vm.open(Screen.Bag) },
                                    onLockAfter = vm::setLockAfter,
                                    onExport = { askExport = true },
                                    onImport = {
                                        vm.externalActivity = true
                                        openBackup.launch(arrayOf("*/*"))
                                    },
                                    onErase = { askErase = true },
                                )
                            }
                        }
                        BottomBar(vm.tab) { vm.tab = it }
                    }
                    is Screen.EditEvent -> {
                        val existing = s.id?.let { id -> data.events.firstOrNull { it.id == id } }
                        val initial = remember(s) { existing ?: draftEvent(Protocol.get(s.protocolKey), g) }
                        EventEditScreen(
                            initial, isNew = existing == null, g = g,
                            onCancel = { vm.back() },
                            onSave = ::saveEvent,
                            onDelete = { vm.deleteEvent(initial.id); vm.back() },
                            onAddResult = {
                                vm.back()
                                openNewResult()
                            },
                            scan = vm.scannedAppointment,
                            onScanCamera = { startScan(forResult = false, camera = true) },
                            onScanGallery = { startScan(forResult = false, camera = false) },
                            onAddToCalendar = onAddToCalendar,
                        )
                    }
                    is Screen.EditResult -> {
                        val existing = s.id?.let { id -> data.results.firstOrNull { it.id == id } }
                        val initial = remember(s) { existing ?: Result(date = LocalDate.now()) }
                        ResultEditScreen(
                            initial, isNew = existing == null, thumbs = { vm.thumbnail(it) }, newAttachments = newAttachments,
                            onPickPhoto = {
                                vm.externalActivity = true
                                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onTakePhoto = {
                                val uri = vm.newCameraUri()
                                cameraUri = uri
                                vm.externalActivity = true
                                takePhoto.launch(uri)
                            },
                            onPickFile = {
                                vm.externalActivity = true
                                pickFile.launch(arrayOf("application/pdf", "image/*"))
                            },
                            onCancel = { newAttachments.clear(); vm.back() },
                            onSave = { r, removed ->
                                vm.saveResult(r, removed)
                                newAttachments.clear()
                                vm.back()
                            },
                            scan = vm.scannedResult,
                            onScanCamera = { startScan(forResult = true, camera = true) },
                            onScanGallery = { startScan(forResult = true, camera = false) },
                        )
                    }
                    is Screen.ViewResult -> data.results.firstOrNull { it.id == s.id }?.let { r ->
                        ResultViewScreen(
                            r, g?.dueDate, { vm.thumbnail(it) },
                            onBack = { vm.back() },
                            onEdit = { newAttachments.clear(); vm.clearScans(); vm.open(Screen.EditResult(r.id)) },
                            onOpenAttachment = { vm.open(Screen.ViewAttachment(it)) },
                            onDelete = { vm.deleteResult(r.id); vm.back() },
                        )
                    }
                    is Screen.ViewAttachment -> AttachmentViewer(
                        s.attachment, { vm.renderAttachment(it) },
                        onBack = { vm.back() },
                        onShare = { scope.launch { vm.shareUri(s.attachment)?.let(onShare) } },
                    )
                    is Screen.EditMeasurement -> {
                        val existing = s.id?.let { id -> data.measurements.firstOrNull { it.id == id } }
                        val initial = remember(s) { existing ?: Measurement(date = LocalDate.now()) }
                        MeasurementEditScreen(
                            initial, isNew = existing == null,
                            onCancel = { vm.back() },
                            onSave = { vm.saveMeasurement(it); vm.back() },
                            onDelete = { vm.deleteMeasurement(initial.id); vm.back() },
                        )
                    }
                    Screen.EditProfile -> ProfileEditScreen(data.profile, onCancel = { vm.back() }, onSave = { vm.saveProfile(it); vm.back() })
                    Screen.Bag -> BagScreen(data.bag, onBack = { vm.back() }, onChange = vm::setBag)
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp, start = 16.dp, end = 16.dp)) {
            Snackbar(it, containerColor = P.Text, contentColor = P.Surface)
        }
        vm.busy?.let { BusyOverlay(it) }
    }

    protocolOpen?.let { item ->
        val d = vm.data
        val status = Protocol.status(item, vm.gestation?.weeks ?: 0, d?.events.orEmpty(), d?.skippedProtocol.orEmpty())
        ProtocolDialog(
            item, status, vm.gestation,
            onSchedule = {
                protocolOpen = null
                openEvent(null, item.key)
            },
            onDoneToday = {
                protocolOpen = null
                vm.saveEvent(draftEvent(item, vm.gestation).copy(at = LocalDateTime.now(), done = true, remind = false))
            },
            onSkip = { skip ->
                protocolOpen = null
                vm.skipProtocol(item.key, skip)
            },
            onDismiss = { protocolOpen = null },
        )
    }
    if (askExport) {
        PassphraseDialog(
            "Protect your backup",
            "Choose a passphrase for this backup. You’ll need it to restore. It can’t be recovered if lost.",
            "Continue", repeat = true,
            onConfirm = { pass ->
                askExport = false
                exportPass = pass
                vm.externalActivity = true
                createBackup.launch(vm.backupFileName())
            },
            onDismiss = { askExport = false },
        )
    }
    importUri?.let { uri ->
        PassphraseDialog(
            "Restore backup", "Enter the passphrase used when this backup was made.", "Restore", repeat = false,
            onConfirm = { pass ->
                importUri = null
                vm.importBackup(uri, pass)
            },
            onDismiss = { importUri = null },
        )
    }
    if (scanChooser) {
        ScanChooser(
            onResult = { camera ->
                scanChooser = false
                openNewResult()
                startScan(forResult = true, camera = camera)
            },
            onAppointment = { camera ->
                scanChooser = false
                openEvent(null)
                startScan(forResult = false, camera = camera)
            },
            onDismiss = { scanChooser = false },
        )
    }
    if (askErase) EraseDialog(onConfirm = { askErase = false; vm.eraseAll() }, onDismiss = { askErase = false })
}

@Composable
private fun BottomBar(tab: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = P.Surface, tonalElevation = 0.dp) {
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = t == tab,
                onClick = { onSelect(t) },
                icon = {
                    Icon(
                        when (t) {
                            Tab.HOME -> Icons.Rounded.Favorite
                            Tab.SCHEDULE -> Icons.Rounded.CalendarMonth
                            Tab.RESULTS -> Icons.Rounded.Science
                            Tab.HEALTH -> Icons.Rounded.MonitorHeart
                            Tab.MORE -> Icons.Rounded.MoreHoriz
                        },
                        t.label,
                    )
                },
                label = { Text(t.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = P.RoseDeep,
                    selectedTextColor = P.RoseDeep,
                    indicatorColor = P.Rose.copy(alpha = 0.14f),
                    unselectedIconColor = P.Text3,
                    unselectedTextColor = P.Text3,
                ),
            )
        }
    }
}
