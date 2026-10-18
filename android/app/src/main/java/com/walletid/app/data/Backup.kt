package com.walletid.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

class WrongPassphraseException : Exception("Wrong passphrase for this backup")
class NotABackupException : Exception("That file isn’t a Wallet ID backup")

/**
 * Encrypted .idvault backups, byte-compatible with the web app:
 * {format:"id-vault", version:1, salt, iterations, check:{iv,data},
 *  items:[{id, box:{iv,data}}], images:[{id:"<docId>:<side>", type, box}]}
 */
object Backup {
    private const val CHECK_VALUE = "\"id-vault-ok\"" // JSON.stringify("id-vault-ok")
    private val b64e = Base64.getEncoder()
    private val b64d = Base64.getDecoder()

    data class Contents(val docs: List<Doc>, val images: Map<Pair<String, Side>, ByteArray>)

    fun export(
        docs: List<Doc>,
        image: (String, Side) -> ByteArray?,
        passphrase: String,
        iterations: Int = PassphraseCrypto.ITERATIONS,
    ): String {
        val salt = PassphraseCrypto.randomBytes(16)
        val key = PassphraseCrypto.deriveKey(passphrase, salt, iterations)
        fun box(bytes: ByteArray): JSONObject {
            val (iv, data) = PassphraseCrypto.encrypt(key, bytes)
            return JSONObject().put("iv", b64e.encodeToString(iv)).put("data", b64e.encodeToString(data))
        }

        val items = JSONArray()
        val images = JSONArray()
        for (d in docs) {
            items.put(JSONObject().put("id", d.id).put("box", box(JSONObject(d.fields).toString().toByteArray())))
            for (side in Side.entries) {
                val bytes = image(d.id, side) ?: continue
                images.put(
                    JSONObject().put("id", "${d.id}:${side.key}").put("type", sniffMime(bytes)).put("box", box(bytes)),
                )
            }
        }
        return JSONObject()
            .put("format", "id-vault")
            .put("version", 1)
            .put("salt", b64e.encodeToString(salt))
            .put("iterations", iterations)
            .put("check", box(CHECK_VALUE.toByteArray()))
            .put("items", items)
            .put("images", images)
            .toString()
    }

    fun parse(text: String, passphrase: String): Contents {
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (json == null || json.optString("format") != "id-vault") throw NotABackupException()
        val key = PassphraseCrypto.deriveKey(passphrase, b64d.decode(json.getString("salt")), json.getInt("iterations"))
        fun open(box: JSONObject): ByteArray =
            PassphraseCrypto.decrypt(key, b64d.decode(box.getString("iv")), b64d.decode(box.getString("data")))

        val check = runCatching { String(open(json.getJSONObject("check"))) }.getOrNull()
        if (check != CHECK_VALUE) throw WrongPassphraseException()

        val items = json.optJSONArray("items") ?: JSONArray()
        val docs = (0 until items.length()).map { i ->
            val o = items.getJSONObject(i)
            val fields = JSONObject(String(open(o.getJSONObject("box")), Charsets.UTF_8)).toStringMap()
            Doc(o.getString("id"), DocTypes.normalize(fields))
        }
        val imgs = json.optJSONArray("images") ?: JSONArray()
        val images = buildMap {
            for (i in 0 until imgs.length()) {
                val o = imgs.getJSONObject(i)
                val parts = o.getString("id").split(":")
                val side = Side.entries.firstOrNull { it.key == parts.getOrNull(1) } ?: continue
                put(parts[0] to side, open(o.getJSONObject("box")))
            }
        }
        return Contents(docs, images)
    }

    fun sniffMime(bytes: ByteArray): String = when {
        bytes.size > 3 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "image/png"
        bytes.size > 12 && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
        else -> "image/jpeg"
    }
}
