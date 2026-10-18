package com.walletid.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class Side(val key: String) { FRONT("front"), BACK("back") }

/**
 * Encrypted on-disk store. Everything under [dir] is ciphertext:
 *   vault.bin              all documents as encrypted JSON
 *   images/<id>_<side>.bin each photo, encrypted separately
 */
class Vault(private val dir: File, private val cipher: VaultCipher) {
    private val vaultFile get() = File(dir, "vault.bin")
    private val imagesDir get() = File(dir, "images").apply { mkdirs() }

    @Synchronized
    fun load(): List<Doc> {
        if (!vaultFile.exists()) return emptyList()
        val json = JSONObject(String(cipher.decrypt(vaultFile.readBytes()), Charsets.UTF_8))
        val arr = json.getJSONArray("docs")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Doc(o.getString("id"), DocTypes.normalize(o.getJSONObject("fields").toStringMap()))
        }
    }

    @Synchronized
    fun saveAll(docs: List<Doc>) {
        val arr = JSONArray()
        docs.forEach { d -> arr.put(JSONObject().put("id", d.id).put("fields", JSONObject(d.fields))) }
        val bytes = JSONObject().put("version", 1).put("docs", arr).toString().toByteArray(Charsets.UTF_8)
        writeAtomically(vaultFile, cipher.encrypt(bytes))
    }

    private fun imageFile(id: String, side: Side) = File(imagesDir, "${id}_${side.key}.bin")

    fun hasImage(id: String, side: Side) = imageFile(id, side).exists()

    fun loadImage(id: String, side: Side): ByteArray? =
        imageFile(id, side).takeIf { it.exists() }?.let { cipher.decrypt(it.readBytes()) }

    fun saveImage(id: String, side: Side, bytes: ByteArray?) {
        val f = imageFile(id, side)
        if (bytes == null) f.delete() else writeAtomically(f, cipher.encrypt(bytes))
    }

    fun deleteImages(id: String) = Side.entries.forEach { imageFile(id, it).delete() }

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

fun JSONObject.toStringMap(): Map<String, String> =
    keys().asSequence().associateWith { k -> opt(k)?.toString().orEmpty() }
