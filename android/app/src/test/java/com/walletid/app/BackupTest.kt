package com.walletid.app

import com.walletid.app.data.Backup
import com.walletid.app.data.Doc
import com.walletid.app.data.Side
import com.walletid.app.data.WrongPassphraseException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private fun fixture() = javaClass.classLoader!!.getResource("web-backup.json")!!.readText()

    @Test
    fun readsBackupMadeByTheWebApp() {
        val contents = Backup.parse(fixture(), "correct horse battery")

        val cnh = contents.docs.single { it.id == "doc-cnh" }
        assertEquals("cnh", cnh["type"])
        assertEquals("Maria Silva Santos", cnh["holder"])
        assertEquals("123.456.789-01", cnh["cpf"])
        assertEquals("Renovar com antecedência", cnh["notes"])

        // Items saved by the very first web version get mapped onto the catalogue.
        val legacy = contents.docs.single { it.id == "doc-legacy" }
        assertEquals("passport", legacy["type"])

        val img = contents.images.getValue("doc-cnh" to Side.FRONT)
        assertArrayEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4), img)
        assertEquals("image/png", Backup.sniffMime(img))
    }

    @Test(expected = WrongPassphraseException::class)
    fun rejectsWrongPassphrase() {
        Backup.parse(fixture(), "not the passphrase")
    }

    @Test
    fun roundTripsItsOwnExport() {
        val docs = listOf(
            Doc("a", mapOf("type" to "rg", "holder" to "João", "number" to "12.345.678-9")),
            Doc("b", mapOf("type" to "creci", "number" to "123456-F", "region" to "CRECI-SP")),
        )
        val photo = ByteArray(5000) { (it % 251).toByte() }
        val text = Backup.export(docs, { id, side -> if (id == "a" && side == Side.BACK) photo else null }, "s3cret-pass", iterations = 1000)

        val back = Backup.parse(text, "s3cret-pass")
        assertEquals(docs.toSet(), back.docs.toSet())
        assertEquals(setOf("a" to Side.BACK), back.images.keys)
        assertArrayEquals(photo, back.images.getValue("a" to Side.BACK))
        assertTrue(text.contains("\"format\":\"id-vault\""))
    }
}
