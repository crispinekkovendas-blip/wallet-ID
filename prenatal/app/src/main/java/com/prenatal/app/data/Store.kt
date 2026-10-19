package com.prenatal.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64

/**
 * Encrypted on-disk store. Everything under [dir] is ciphertext:
 *   data.bin            the whole pregnancy record as encrypted JSON
 *   files/<id>.bin      each attached photo or PDF, encrypted separately
 */
class Store(private val dir: File, private val cipher: VaultCipher) {
    private val dataFile get() = File(dir, "data.bin")
    private val filesDir get() = File(dir, "files").apply { mkdirs() }

    @Synchronized
    fun load(): PregnancyData? {
        if (!dataFile.exists()) return null
        return PregnancyData.fromJson(JSONObject(String(cipher.decrypt(dataFile.readBytes()), Charsets.UTF_8)))
    }

    @Synchronized
    fun save(data: PregnancyData) = writeAtomically(dataFile, cipher.encrypt(data.toJson().toString().toByteArray(Charsets.UTF_8)))

    fun loadFile(id: String): ByteArray? = File(filesDir, "$id.bin").takeIf { it.exists() }?.let { cipher.decrypt(it.readBytes()) }

    fun saveFile(id: String, bytes: ByteArray) = writeAtomically(File(filesDir, "$id.bin"), cipher.encrypt(bytes))

    fun deleteFile(id: String) {
        File(filesDir, "$id.bin").delete()
    }

    fun eraseAll() {
        dir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }
}

class WrongPassphraseException : Exception("Wrong passphrase for this backup")
class NotABackupException : Exception("That file isn’t a Prenatal backup")

/**
 * Encrypted backup file (.prenatal): the record and every attachment, sealed with a passphrase
 * (PBKDF2-SHA256 → AES-256-GCM), same scheme as Wallet ID.
 */
object Backup {
    private const val FORMAT = "prenatal-backup"
    private const val CHECK = "prenatal-ok"
    private val b64e = Base64.getEncoder()
    private val b64d = Base64.getDecoder()

    fun export(
        data: PregnancyData,
        file: (String) -> ByteArray?,
        passphrase: String,
        iterations: Int = PassphraseCrypto.ITERATIONS,
    ): String {
        val salt = PassphraseCrypto.randomBytes(16)
        val key = PassphraseCrypto.deriveKey(passphrase, salt, iterations)
        fun box(bytes: ByteArray): JSONObject {
            val (iv, ct) = PassphraseCrypto.encrypt(key, bytes)
            return JSONObject().put("iv", b64e.encodeToString(iv)).put("data", b64e.encodeToString(ct))
        }
        val files = JSONArray()
        data.results.flatMap { it.attachments }.forEach { a ->
            file(a.id)?.let { files.put(JSONObject().put("id", a.id).put("box", box(it))) }
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", 1)
            .put("salt", b64e.encodeToString(salt))
            .put("iterations", iterations)
            .put("check", box(CHECK.toByteArray()))
            .put("data", box(data.toJson().toString().toByteArray(Charsets.UTF_8)))
            .put("files", files)
            .toString()
    }

    fun parse(text: String, passphrase: String): Pair<PregnancyData, Map<String, ByteArray>> {
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (json == null || json.optString("format") != FORMAT) throw NotABackupException()
        val key = PassphraseCrypto.deriveKey(passphrase, b64d.decode(json.getString("salt")), json.getInt("iterations"))
        fun open(box: JSONObject) = PassphraseCrypto.decrypt(key, b64d.decode(box.getString("iv")), b64d.decode(box.getString("data")))
        val check = runCatching { String(open(json.getJSONObject("check"))) }.getOrNull()
        if (check != CHECK) throw WrongPassphraseException()
        val data = PregnancyData.fromJson(JSONObject(String(open(json.getJSONObject("data")), Charsets.UTF_8)))
        val arr = json.optJSONArray("files") ?: JSONArray()
        val files = (0 until arr.length()).associate { i ->
            val o = arr.getJSONObject(i)
            o.getString("id") to open(o.getJSONObject("box"))
        }
        return data to files
    }
}
