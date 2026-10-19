package com.walletid.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.walletid.app.data.Side
import com.walletid.app.ui.BusyOverlay
import com.walletid.app.ui.CardScreen
import com.walletid.app.ui.EditScreen
import com.walletid.app.ui.EraseDialog
import com.walletid.app.ui.LockScreen
import com.walletid.app.ui.Palette
import com.walletid.app.ui.PassphraseDialog
import com.walletid.app.ui.SettingsScreen
import com.walletid.app.ui.WalletScreen
import com.walletid.app.ui.WalletTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var stoppedAt = 0L
    private var authError by mutableStateOf<String?>(null)
    private var promptShowing = false

    private val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep documents out of screenshots, screen recordings and the recent-apps preview.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            WalletTheme {
                AppRoot(
                    vm = vm,
                    canAuthenticate = canAuthenticate(),
                    authError = authError,
                    onUnlock = ::authenticate,
                    onOpenSecuritySettings = { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                    onShare = ::share,
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
        // busy is set while the vault opens right after a successful unlock.
        if (promptShowing || vm.unlocked || vm.busy != null) return
        promptShowing = true
        authError = null
        val prompt = BiometricPrompt(
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
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Wallet ID")
                .setSubtitle("Confirm it’s you to open your documents")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    private fun share(uri: Uri) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = contentResolver.getType(uri) ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        vm.externalActivity = true
        startActivity(Intent.createChooser(send, "Share document photo"))
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
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    var photoSide by remember { mutableStateOf(Side.FRONT) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var qrFor by remember { mutableStateOf<String?>(null) }
    var askExport by remember { mutableStateOf(false) }
    var exportPass by remember { mutableStateOf<String?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var askErase by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.pickImage(photoSide, uri)
    }
    val pickQr = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.pickQrImage(uri)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraUri?.let { if (ok) vm.pickImage(photoSide, it) }
    }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pass = exportPass
        exportPass = null
        if (uri != null && pass != null) vm.exportBackup(uri, pass)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importUri = uri
    }

    BackHandler(enabled = vm.unlocked && vm.stack.size > 1) { vm.back() }
    BackHandler(enabled = qrFor != null) { qrFor = null }

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        if (!vm.unlocked) {
            LockScreen(canAuthenticate, authError, onUnlock, onOpenSecuritySettings)
        } else {
            AnimatedContent(
                targetState = vm.screen,
                transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(320)) },
                label = "screens",
            ) { s ->
                when (s) {
                    Screen.Wallet -> WalletScreen(
                        docs = vm.visibleDocs,
                        thumbs = vm.thumbs,
                        totalCount = vm.docs.size,
                        query = vm.query,
                        onQuery = { vm.query = it },
                        onOpen = { vm.open(Screen.Detail(it)) },
                        onAdd = vm::startAdd,
                        onSettings = { vm.open(Screen.Settings) },
                        onLock = vm::lock,
                        onImport = {
                            vm.externalActivity = true
                            openBackup.launch(arrayOf("*/*"))
                        },
                    )
                    Screen.Edit -> vm.edit?.let { session ->
                        EditScreen(
                            session = session,
                            onCancel = { vm.back() },
                            onSave = vm::save,
                            onChoosePhoto = { side ->
                                photoSide = side
                                vm.externalActivity = true
                                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onTakePhoto = { side ->
                                photoSide = side
                                val uri = vm.newCameraUri()
                                cameraUri = uri
                                vm.externalActivity = true
                                takePhoto.launch(uri)
                            },
                            onRemovePhoto = vm::removeImage,
                            onRotatePhoto = vm::rotateImage,
                            onLabel = vm::setLabel,
                            onQrFromImage = {
                                vm.externalActivity = true
                                pickQr.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onQrText = vm::setQrText,
                        )
                    }
                    is Screen.Detail -> vm.doc(s.id)?.let { doc ->
                        val version = vm.imagesVersion
                        var flipped by remember(doc.id) { mutableStateOf(false) }
                        val front by produceState(vm.thumbs[doc.id], doc.id, version) { value = vm.image(doc.id, Side.FRONT, 1800) ?: value }
                        val back by produceState<ImageBitmap?>(null, doc.id, version) { value = vm.image(doc.id, Side.BACK, 1800) }
                        val hasQr = vm.hasQr(doc.id)
                        val qrPicture by produceState<ImageBitmap?>(null, doc.id, version, hasQr) {
                            value = if (hasQr) vm.image(doc.id, Side.QR, 1600) else null
                        }
                        CardScreen(
                            doc = doc,
                            front = front,
                            back = back,
                            hasQr = hasQr,
                            qr = vm.qrCode(doc.id),
                            qrPicture = qrPicture,
                            showQr = qrFor == doc.id,
                            flipped = flipped,
                            onFlip = { flipped = !flipped },
                            onShowQr = { qrFor = if (it) doc.id else null },
                            onBack = { vm.back() },
                            onEdit = { vm.startEdit(doc.id) },
                            onShare = { side -> scope.launch { vm.sharePhotoUri(doc.id, side)?.let(onShare) } },
                            onDelete = { vm.delete(doc.id) },
                        )
                    }
                    Screen.Settings -> SettingsScreen(
                        lockAfterSeconds = vm.lockAfterSeconds,
                        version = BuildConfig.VERSION_NAME,
                        onLockAfter = vm::setLockAfter,
                        onExport = { askExport = true },
                        onImport = {
                            vm.externalActivity = true
                            openBackup.launch(arrayOf("*/*"))
                        },
                        onErase = { askErase = true },
                        onBack = { vm.back() },
                    )
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)) {
            Snackbar(it, containerColor = Palette.Surface2, contentColor = Palette.Text)
        }
        vm.busy?.let { BusyOverlay(it) }
    }

    if (askExport) {
        PassphraseDialog(
            title = "Protect your backup",
            text = "Choose a passphrase for this backup file. You’ll need it to restore. It can’t be recovered if lost.",
            confirm = "Continue",
            repeat = true,
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
            title = "Import backup",
            text = "Enter the passphrase that was used when this backup was made.",
            confirm = "Import",
            repeat = false,
            onConfirm = { pass ->
                importUri = null
                vm.importBackup(uri, pass)
            },
            onDismiss = { importUri = null },
        )
    }
    if (askErase) {
        EraseDialog(onConfirm = { askErase = false; vm.eraseAll() }, onDismiss = { askErase = false })
    }
}
