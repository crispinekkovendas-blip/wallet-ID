package com.prenatal.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prenatal.app.data.Attachment
import com.prenatal.app.data.Backup
import com.prenatal.app.data.Event
import com.prenatal.app.data.Gestation
import com.prenatal.app.data.KeystoreCipher
import com.prenatal.app.data.Measurement
import com.prenatal.app.data.NotABackupException
import com.prenatal.app.data.PregnancyData
import com.prenatal.app.data.Profile
import com.prenatal.app.data.Result
import com.prenatal.app.data.Store
import com.prenatal.app.data.WrongPassphraseException
import com.prenatal.app.reminders.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

enum class Tab(val label: String) { HOME("Today"), SCHEDULE("Schedule"), RESULTS("Results"), HEALTH("Health"), MORE("More") }

sealed interface Screen {
    data object Tabs : Screen
    data class EditEvent(val id: String?, val protocolKey: String? = null) : Screen
    data class EditResult(val id: String?) : Screen
    data class ViewResult(val id: String) : Screen
    data class ViewAttachment(val attachment: Attachment) : Screen
    data class EditMeasurement(val id: String?) : Screen
    data object EditProfile : Screen
    data object Bag : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(File(app.filesDir, "store"), KeystoreCipher())
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var unlocked by mutableStateOf(false)
        private set
    /** null until unlocked; a record without a due date means setup hasn't happened yet. */
    var data by mutableStateOf<PregnancyData?>(null)
        private set
    var tab by mutableStateOf(Tab.HOME)
    val stack = mutableStateListOf<Screen>(Screen.Tabs)
    var busy by mutableStateOf<String?>(null)
        private set
    var lockAfterSeconds by mutableIntStateOf(prefs.getInt("lockAfter", 60))
        private set
    /** True while another app (picker, camera, share sheet) is in front, so we don't lock. */
    var externalActivity = false

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    fun say(msg: String) { messages.tryEmit(msg) }

    val screen: Screen get() = stack.last()
    val gestation: Gestation? get() = data?.profile?.dueDate?.let { Gestation.fromDueDate(it) }

    // ---------- Lock ----------

    fun onAuthenticated() {
        viewModelScope.launch {
            busy = "Opening…"
            try {
                data = withContext(Dispatchers.IO) { store.load() } ?: PregnancyData()
                unlocked = true
            } catch (e: Exception) {
                say("Couldn’t open your data: ${e.message}")
            } finally {
                busy = null
            }
        }
    }

    fun lock() {
        unlocked = false
        data = null
        stack.clear()
        stack.add(Screen.Tabs)
        clearCache()
    }

    fun setLockAfter(seconds: Int) {
        lockAfterSeconds = seconds
        prefs.edit().putInt("lockAfter", seconds).apply()
    }

    // ---------- Navigation ----------

    fun open(s: Screen) { stack.add(s) }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    // ---------- Saving ----------

    /** Applies a change, saves it encrypted and refreshes reminders. */
    fun update(message: String? = null, change: (PregnancyData) -> PregnancyData) {
        val current = data ?: return
        val next = change(current)
        data = next
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    store.save(next)
                    Reminders.reschedule(getApplication(), next.events)
                }
                message?.let(::say)
            } catch (e: Exception) {
                say("Couldn’t save: ${e.message}")
            }
        }
    }

    fun setDueDate(due: LocalDate, motherName: String, babyName: String) = update {
        it.copy(profile = it.profile.copy(dueDate = due, motherName = motherName.trim(), babyName = babyName.trim()))
    }

    fun saveProfile(p: Profile) = update("Saved") { it.copy(profile = p) }

    fun saveEvent(e: Event) = update("Saved") { d ->
        d.copy(events = (d.events.filterNot { it.id == e.id } + e).sortedBy { it.at })
    }

    fun deleteEvent(id: String) = update("Deleted") { d -> d.copy(events = d.events.filterNot { it.id == id }) }

    fun toggleDone(id: String) = update { d -> d.copy(events = d.events.map { if (it.id == id) it.copy(done = !it.done) else it }) }

    fun skipProtocol(key: String, skip: Boolean) = update { d ->
        d.copy(skippedProtocol = if (skip) d.skippedProtocol + key else d.skippedProtocol - key)
    }

    fun saveMeasurement(m: Measurement) = update("Saved") { d ->
        d.copy(measurements = (d.measurements.filterNot { it.id == m.id } + m).sortedBy { it.date })
    }

    fun deleteMeasurement(id: String) = update("Deleted") { d -> d.copy(measurements = d.measurements.filterNot { it.id == id }) }

    fun setBag(items: List<com.prenatal.app.data.CheckItem>) = update { it.copy(bag = items) }

    // ---------- Results & attachments ----------

    /** New files picked while editing a result, kept in memory until Save. */
    val pendingFiles = HashMap<String, ByteArray>()

    fun saveResult(r: Result, removed: List<Attachment>) {
        val files = r.attachments.mapNotNull { a -> pendingFiles[a.id]?.let { a.id to it } }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    files.forEach { (id, bytes) -> store.saveFile(id, bytes) }
                    removed.forEach { store.deleteFile(it.id) }
                }
                files.forEach { pendingFiles.remove(it.first) }
                update("Saved") { d -> d.copy(results = (d.results.filterNot { it.id == r.id } + r).sortedByDescending { it.date }) }
            } catch (e: Exception) {
                say("Couldn’t save: ${e.message}")
            }
        }
    }

    fun deleteResult(id: String) {
        val r = data?.results?.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { r.attachments.forEach { store.deleteFile(it.id) } }
            update("Deleted") { d -> d.copy(results = d.results.filterNot { it.id == id }) }
        }
    }

    /** Reads a picked photo or PDF. Photos are resized so the record stays light. */
    suspend fun readPicked(uri: Uri): Attachment? = withContext(Dispatchers.IO) {
        try {
            val resolver = getApplication<Application>().contentResolver
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "file"
            var bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            var finalMime = mime
            if (mime.startsWith("image/")) {
                shrinkImage(bytes)?.let { bytes = it; finalMime = "image/jpeg" }
            }
            val a = Attachment(name = name, mime = finalMime)
            pendingFiles[a.id] = bytes
            a
        } catch (e: Exception) {
            say("Couldn’t read that file")
            null
        }
    }

    private fun shrinkImage(bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / (sample * 2) >= 2400) sample *= 2
        var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val scale = 2400f / max(bmp.width, bmp.height)
        if (scale < 1f) bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).roundToInt(), (bmp.height * scale).roundToInt(), true)
        return ByteArrayOutputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it); it.toByteArray() }
    }

    private suspend fun fileBytes(id: String): ByteArray? = pendingFiles[id] ?: withContext(Dispatchers.IO) { store.loadFile(id) }

    /** Pages of an attachment as images: the photo itself, or each page of a PDF rendered here on the phone. */
    suspend fun renderAttachment(a: Attachment): List<ImageBitmap> {
        val bytes = fileBytes(a.id) ?: return emptyList()
        return withContext(Dispatchers.IO) {
            if (a.mime == "application/pdf") renderPdf(bytes) else {
                listOfNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap())
            }
        }
    }

    private fun renderPdf(bytes: ByteArray): List<ImageBitmap> {
        val tmp = File(getApplication<Application>().cacheDir, "view-${System.nanoTime()}.pdf")
        try {
            tmp.writeBytes(bytes)
            ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { pdf ->
                    return (0 until minOf(pdf.pageCount, 30)).map { i ->
                        pdf.openPage(i).use { page ->
                            val w = 1600
                            val h = (w.toFloat() * page.height / page.width).roundToInt()
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp.asImageBitmap()
                        }
                    }
                }
            }
        } finally {
            tmp.delete()
        }
    }

    suspend fun thumbnail(a: Attachment): ImageBitmap? = renderAttachment(a).firstOrNull()

    // ---------- Share ----------

    private val shareDir get() = File(getApplication<Application>().cacheDir, "share")

    private fun clearCache() {
        shareDir.deleteRecursively()
        File(getApplication<Application>().cacheDir, "camera").deleteRecursively()
        pendingFiles.clear()
    }

    suspend fun shareUri(a: Attachment): Uri? {
        val bytes = fileBytes(a.id) ?: return null
        return withContext(Dispatchers.IO) {
            shareDir.deleteRecursively()
            shareDir.mkdirs()
            val safe = a.name.replace(Regex("[^A-Za-z0-9À-ÿ._-]"), "_").ifEmpty { "file" }
            val f = File(shareDir, safe)
            f.writeBytes(bytes)
            FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.files", f)
        }
    }

    fun newCameraUri(): Uri {
        val dir = File(getApplication<Application>().cacheDir, "camera").apply { mkdirs() }
        return FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.files", File(dir, "photo-${System.currentTimeMillis()}.jpg"))
    }

    // ---------- Backup ----------

    fun exportBackup(uri: Uri, passphrase: String) {
        val current = data ?: return
        viewModelScope.launch {
            busy = "Encrypting backup…"
            try {
                withContext(Dispatchers.Default) {
                    val text = Backup.export(current, { store.loadFile(it) }, passphrase)
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                        ?: error("Couldn’t write the file")
                }
                say("Encrypted backup saved")
            } catch (e: Exception) {
                say("Export failed: ${e.message}")
            } finally {
                busy = null
            }
        }
    }

    fun importBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            busy = "Restoring backup…"
            try {
                val restored = withContext(Dispatchers.Default) {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: error("Couldn’t open the file")
                    val (d, files) = Backup.parse(text, passphrase)
                    files.forEach { (id, bytes) -> store.saveFile(id, bytes) }
                    store.save(d)
                    Reminders.reschedule(getApplication(), d.events)
                    d
                }
                data = restored
                say("Backup restored")
            } catch (e: WrongPassphraseException) {
                say(e.message!!)
            } catch (e: NotABackupException) {
                say(e.message!!)
            } catch (e: Exception) {
                say("Import failed: ${e.message}")
            } finally {
                busy = null
            }
        }
    }

    fun eraseAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                store.eraseAll()
                KeystoreCipher.deleteKey()
                Reminders.reschedule(getApplication(), emptyList())
            }
            data = PregnancyData()
            stack.clear()
            stack.add(Screen.Tabs)
            tab = Tab.HOME
            say("All data erased")
        }
    }

    fun backupFileName() = "prenatal-${LocalDate.now()}.prenatal"
}
