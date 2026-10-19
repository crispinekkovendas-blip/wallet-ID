package com.walletid.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walletid.app.data.Backup
import com.walletid.app.data.Doc
import com.walletid.app.data.DocTypes
import com.walletid.app.data.Images
import com.walletid.app.data.KeystoreCipher
import com.walletid.app.data.NotABackupException
import com.walletid.app.data.Qr
import com.walletid.app.data.QrModules
import com.walletid.app.data.Side
import com.walletid.app.data.Vault
import com.walletid.app.data.WrongPassphraseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

sealed interface Screen {
    data object Wallet : Screen
    data object Edit : Screen
    data class Detail(val id: String) : Screen
    data object Settings : Screen
}

/** Unsaved changes while adding or editing a card. */
class EditSession(val id: String?, typeKey: String, initial: Map<String, String>) {
    var typeKey by mutableStateOf(typeKey)
    val fields: SnapshotStateMap<String, String> = mutableStateMapOf<String, String>().apply { putAll(initial) }
    /** side -> new bytes, or null to remove. Absent = unchanged. */
    val imageChanges = mutableStateMapOf<Side, ByteArray?>()
    val previews = mutableStateMapOf<Side, ImageBitmap?>()
    var scanning by mutableStateOf(false)

    val title: String get() = fields["title"].orEmpty()
    val qrCode: QrModules? get() = fields["qrModules"]?.let { QrModules.parse(fields["qr"].orEmpty(), it) }
    /** An older card may only have a picture of its QR code. */
    val hasQr: Boolean get() = qrCode != null || previews[Side.QR] != null

    fun asDoc() = Doc(id ?: "draft", fields.toMap() + ("type" to typeKey))
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = Vault(File(app.filesDir, "vault"), KeystoreCipher())
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var unlocked by mutableStateOf(false)
        private set
    var docs by mutableStateOf<List<Doc>>(emptyList())
        private set
    val stack = mutableStateListOf<Screen>(Screen.Wallet)
    var query by mutableStateOf("")
    var edit by mutableStateOf<EditSession?>(null)
        private set
    var busy by mutableStateOf<String?>(null)
        private set
    /** Front photos for the wallet, by document id (null = no photo). */
    val thumbs = mutableStateMapOf<String, ImageBitmap?>()
    /** Bumped whenever stored photos change, so image loaders refresh. */
    var imagesVersion by mutableIntStateOf(0)
        private set
    var lockAfterSeconds by mutableIntStateOf(prefs.getInt("lockAfter", 0))
        private set

    /** True while another app (photo picker, camera, share sheet) is in front, so we don't lock. */
    var externalActivity = false

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private fun say(msg: String) { messages.tryEmit(msg) }

    private val imageCache = HashMap<String, ImageBitmap>()

    val screen: Screen get() = stack.last()

    // ---------- Lock ----------

    fun onAuthenticated() {
        viewModelScope.launch {
            busy = "Opening your wallet…"
            try {
                docs = withContext(Dispatchers.IO) { vault.load() }
                unlocked = true
                docs.forEach { loadThumb(it.id) }
                upgradeSavedCards()
            } catch (e: Exception) {
                say("Couldn’t open the vault: ${e.message}")
            } finally {
                busy = null
            }
        }
    }

    fun lock() {
        unlocked = false
        docs = emptyList()
        edit = null
        query = ""
        imageCache.clear()
        thumbs.clear()
        stack.clear()
        stack.add(Screen.Wallet)
        clearShareCache()
    }

    fun setLockAfter(seconds: Int) {
        lockAfterSeconds = seconds
        prefs.edit().putInt("lockAfter", seconds).apply()
    }

    // ---------- Navigation ----------

    fun open(s: Screen) {
        stack.add(s)
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        if (screen == Screen.Edit) edit = null
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun doc(id: String) = docs.firstOrNull { it.id == id }

    val visibleDocs: List<Doc>
        get() = docs.filter { it.matches(query) }

    // ---------- Images ----------

    suspend fun image(id: String, side: Side, maxSide: Int): ImageBitmap? {
        val key = "$id:${side.key}:$maxSide:$imagesVersion"
        imageCache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            vault.loadImage(id, side)?.let { Images.decode(it, maxSide)?.asImageBitmap() }
        }?.also { imageCache[key] = it }
    }

    private fun loadThumb(id: String) {
        viewModelScope.launch { thumbs[id] = image(id, Side.FRONT, 1000) }
    }

    fun hasQr(id: String) = qrCode(id) != null || vault.hasImage(id, Side.QR)

    fun qrCode(id: String): QrModules? = doc(id)?.let { d -> d.fields["qrModules"]?.let { QrModules.parse(d["qr"], it) } }

    /**
     * Cards saved by earlier versions: turn portrait photos to landscape (trimming margins),
     * and replace a QR picture with its exact module grid so it shows in high definition.
     */
    private fun upgradeSavedCards() {
        if (prefs.getBoolean("cardsUpgradedV2", false)) return
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                docs.map { d ->
                    var changed = false
                    for (side in listOf(Side.FRONT, Side.BACK)) {
                        val bytes = vault.loadImage(d.id, side) ?: continue
                        if (!Images.isPortrait(bytes)) continue
                        val bmp = Images.decode(bytes, 2400) ?: continue
                        vault.saveImage(d.id, side, Images.encode(Images.normalize(bmp)))
                        changed = true
                    }
                    var fields = d.fields
                    if (fields["qrModules"] == null) {
                        val code = vault.loadImage(d.id, Side.QR)?.let { Images.decode(it, 2400) }?.let { Qr.find(it) }
                        if (code != null) {
                            fields = fields + mapOf("qr" to code.text, "qrModules" to code.serialize())
                            vault.saveImage(d.id, Side.QR, null)
                            changed = true
                        }
                    }
                    if (changed) Doc(d.id, fields) else d
                }.also { vault.saveAll(it) }
            }
            docs = updated
            imagesVersion++
            imageCache.clear()
            docs.forEach { loadThumb(it.id) }
            prefs.edit().putBoolean("cardsUpgradedV2", true).apply()
        }
    }

    // ---------- Add / edit ----------

    fun startAdd() {
        edit = EditSession(null, "other", emptyMap())
        stack.add(Screen.Edit)
    }

    fun startEdit(id: String) {
        val d = doc(id) ?: return
        val session = EditSession(id, d.type.key, d.fields - "type")
        if (session.title.isEmpty()) session.fields["title"] = d.title
        edit = session
        stack.add(Screen.Edit)
        viewModelScope.launch {
            for (side in Side.entries) {
                if (side !in session.imageChanges) session.previews[side] = image(id, side, if (side == Side.QR) 800 else 1000)
            }
        }
    }

    /** Quick labels: picking one sets both the name and the document type. */
    fun setLabel(typeKey: String, title: String) {
        val session = edit ?: return
        session.typeKey = typeKey
        session.fields["title"] = title
    }

    fun pickImage(side: Side, uri: Uri) {
        val session = edit ?: return
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { Images.prepare(getApplication<Application>().contentResolver, uri) }
                session.imageChanges[side] = bytes
                session.previews[side] = withContext(Dispatchers.Default) { Images.decode(bytes, 1000)?.asImageBitmap() }
                if (!session.hasQr) scanForQr(session, bytes, quiet = true)
            } catch (e: Exception) {
                say(e.message ?: "Couldn’t read that image")
            }
        }
    }

    /** A separate screenshot of the QR code. If it can't be read we keep the picture as it is. */
    fun pickQrImage(uri: Uri) {
        val session = edit ?: return
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { Images.prepare(getApplication<Application>().contentResolver, uri) }
                if (!scanForQr(session, bytes, quiet = false)) {
                    session.fields.remove("qrModules")
                    session.fields.remove("qr")
                    session.imageChanges[Side.QR] = bytes
                    session.previews[Side.QR] = withContext(Dispatchers.Default) { Images.decode(bytes, 800)?.asImageBitmap() }
                    say("Couldn’t read the code, so the picture was saved as it is. A sharper screenshot gives a high-definition code.")
                }
            } catch (e: Exception) {
                say(e.message ?: "Couldn’t read that image")
            }
        }
    }

    private suspend fun scanForQr(session: EditSession, bytes: ByteArray, quiet: Boolean): Boolean {
        session.scanning = true
        val found = try {
            withContext(Dispatchers.Default) {
                val bmp = Images.decode(bytes, 2400) ?: return@withContext null
                Qr.find(bmp).also { bmp.recycle() }
            }
        } finally {
            session.scanning = false
        }
        if (found == null) {
            if (!quiet) say("No QR code found in that image")
            return false
        }
        setQrCode(session, found)
        say("QR code found · saved in high definition")
        return true
    }

    private fun setQrCode(session: EditSession, code: QrModules) {
        session.fields["qr"] = code.text
        session.fields["qrModules"] = code.serialize()
        // The grid replaces any older QR picture.
        session.imageChanges[Side.QR] = null
        session.previews[Side.QR] = null
    }

    fun setQrText(text: String) {
        val session = edit ?: return
        val clean = text.trim()
        if (clean.isEmpty()) return
        runCatching { Qr.encode(clean) }
            .onSuccess { setQrCode(session, it) }
            .onFailure { say("That text is too long for a QR code") }
    }

    fun removeImage(side: Side) {
        val session = edit ?: return
        session.imageChanges[side] = null
        session.previews[side] = null
        if (side == Side.QR) {
            session.fields.remove("qr")
            session.fields.remove("qrModules")
        }
    }

    /** Turns a photo a quarter turn clockwise. */
    fun rotateImage(side: Side) {
        val session = edit ?: return
        viewModelScope.launch {
            try {
                val current = if (side in session.imageChanges) {
                    session.imageChanges[side]
                } else {
                    session.id?.let { id -> withContext(Dispatchers.IO) { vault.loadImage(id, side) } }
                }
                if (current == null) return@launch
                val turned = withContext(Dispatchers.Default) { Images.rotate(current, 90) }
                session.imageChanges[side] = turned
                session.previews[side] = withContext(Dispatchers.Default) { Images.decode(turned, 1000)?.asImageBitmap() }
            } catch (e: Exception) {
                say(e.message ?: "Couldn’t rotate that image")
            }
        }
    }

    fun save() {
        val session = edit ?: return
        if (session.previews[Side.FRONT] == null) return say("Add a photo of the front of the card")

        val clean = session.fields.mapValues { it.value.trim() }
        val id = session.id ?: UUID.randomUUID().toString()
        val existing = doc(id)
        val title = clean["title"].orEmpty().ifEmpty { DocTypes.get(session.typeKey).abbr }
        // Keys the form removed (e.g. a deleted QR code) must not survive from the old fields.
        val removed = setOf("qr", "qrModules").filter { it !in clean }.toSet()
        val fields = ((existing?.fields.orEmpty() - removed + clean).filterValues { it.isNotEmpty() }) +
            mapOf("type" to session.typeKey, "title" to title)
        val updated = Doc(id, fields)
        val newList = if (existing != null) docs.map { if (it.id == id) updated else it } else docs + updated

        viewModelScope.launch {
            busy = "Encrypting…"
            try {
                withContext(Dispatchers.IO) {
                    vault.saveAll(newList)
                    session.imageChanges.forEach { (side, bytes) -> vault.saveImage(id, side, bytes) }
                }
                docs = newList
                if (session.imageChanges.isNotEmpty()) imagesVersion++
                loadThumb(id)
                edit = null
                stack.removeAt(stack.lastIndex) // the form
                if (existing == null) open(Screen.Detail(id))
                say("Saved securely")
            } catch (e: Exception) {
                say("Save failed: ${e.message}")
            } finally {
                busy = null
            }
        }
    }

    fun delete(id: String) {
        val newList = docs.filterNot { it.id == id }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                vault.saveAll(newList)
                vault.deleteImages(id)
            }
            docs = newList
            thumbs.remove(id)
            imagesVersion++
            stack.removeAll { it is Screen.Detail && it.id == id }
            if (stack.isEmpty()) stack.add(Screen.Wallet)
            say("Card deleted")
        }
    }

    // ---------- Share ----------

    private val shareDir get() = File(getApplication<Application>().cacheDir, "share")

    private fun clearShareCache() {
        shareDir.deleteRecursively()
        File(getApplication<Application>().cacheDir, "camera").deleteRecursively()
    }

    /** Decrypts a photo into a temporary file another app can read. */
    suspend fun sharePhotoUri(id: String, side: Side): Uri? = withContext(Dispatchers.IO) {
        val d = doc(id) ?: return@withContext null
        val bytes = vault.loadImage(id, side) ?: return@withContext null
        clearShareCache()
        shareDir.mkdirs()
        val ext = Backup.sniffMime(bytes).substringAfter('/')
        val name = d.title.replace(Regex("[^A-Za-z0-9À-ÿ_-]"), "_") + if (side == Side.BACK) "-verso" else ""
        val file = File(shareDir, "$name.$ext")
        file.writeBytes(bytes)
        FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.files", file)
    }

    fun newCameraUri(): Uri {
        val dir = File(getApplication<Application>().cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.files", file)
    }

    // ---------- Backup ----------

    fun exportBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            busy = "Encrypting backup…"
            try {
                val current = docs
                withContext(Dispatchers.Default) {
                    val json = Backup.export(current, { id, side -> vault.loadImage(id, side) }, passphrase)
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(json.toByteArray())
                    } ?: error("Couldn’t write the file")
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
            busy = "Decrypting backup…"
            try {
                val current = docs
                val (merged, count) = withContext(Dispatchers.Default) {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: error("Couldn’t open the file")
                    val contents = Backup.parse(text, passphrase)
                    val byId = current.associateBy { it.id }.toMutableMap()
                    contents.docs.forEach { byId[it.id] = it }
                    val all = byId.values.toList()
                    vault.saveAll(all)
                    contents.images.forEach { (key, bytes) -> vault.saveImage(key.first, key.second, bytes) }
                    all to contents.docs.size
                }
                docs = merged
                imagesVersion++
                merged.forEach { loadThumb(it.id) }
                say("Imported $count card${if (count == 1) "" else "s"}")
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
                vault.eraseAll()
                KeystoreCipher.deleteKey()
            }
            docs = emptyList()
            thumbs.clear()
            imagesVersion++
            stack.clear()
            stack.add(Screen.Wallet)
            say("All data erased")
        }
    }

    fun backupFileName() = "wallet-id-${LocalDate.now()}.idvault"
}
