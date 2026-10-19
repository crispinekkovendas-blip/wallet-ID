package com.prenatal.app

import com.prenatal.app.data.EventKind
import com.prenatal.app.data.ResultCategory
import com.prenatal.app.ocr.OcrLine
import com.prenatal.app.ocr.OcrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Realistic OCR text from Brazilian reports and slips (all names and values made up). */
class OcrParserTest {
    private val today = LocalDate.of(2026, 10, 19)

    private val hemograma = """
        LABORATÓRIO SÃO LUCAS
        Paciente: ANA PAULA SOUZA         Data de nascimento: 12/03/1994
        Médico: Dra. Marina Lopes  CRM 123456
        Data da coleta: 14/10/2026     Liberado em: 15/10/2026
        HEMOGRAMA COMPLETO
        Material: Sangue total   Método: Automatizado
        ERITRÓCITOS ........ 4,21 milhões/mm³   3,90 a 5,03
        HEMOGLOBINA ........ 12,1 g/dL   12,0 a 15,5
        HEMATÓCRITO ........ 36,4 %   34,9 a 44,5
        LEUCÓCITOS ......... 8.450 /mm³   3.500 a 10.500
        PLAQUETAS .......... 245.000 /mm³   150.000 a 450.000
        GLICOSE EM JEJUM: 82 mg/dL   70 a 99
        HIV 1 e 2 ......... NÃO REAGENTE
        VDRL: Não reagente
        Página 1 de 2
    """.trimIndent().lines()

    @Test
    fun labReport() {
        val r = OcrParser.parseResult(hemograma, today)
        assertEquals("Blood count (hemograma)", r.title)
        assertEquals(ResultCategory.LAB, r.category)
        assertEquals(LocalDate.of(2026, 10, 14), r.date) // collection date, not the birth date
        assertEquals(
            listOf(
                "Eritrócitos: 4,21 milhões/mm³ (ref 3,90–5,03)",
                "Hemoglobina: 12,1 g/dL (ref 12,0–15,5)",
                "Hematócrito: 36,4 % (ref 34,9–44,5)",
                "Leucócitos: 8.450 /mm³ (ref 3.500–10.500)",
                "Plaquetas: 245.000 /mm³ (ref 150.000–450.000)",
                "Glicose em jejum: 82 mg/dL (ref 70–99)",
                "HIV 1 e 2: Não reagente",
                "VDRL: Não reagente",
            ),
            r.values,
        )
    }

    @Test
    fun ultrasoundReport() {
        val rows = """
            CLÍNICA IMAGEM MULHER
            ULTRASSONOGRAFIA OBSTÉTRICA MORFOLÓGICA DO 2º TRIMESTRE
            Data do exame: 09/10/2026
            Idade gestacional: 23 semanas e 2 dias
            BCF: 148 bpm
            Peso fetal estimado: 640 g
            Placenta: posterior, grau 0
            Líquido amniótico: normal
            Áreas avaliadas sem alterações
        """.trimIndent().lines()
        val r = OcrParser.parseResult(rows, today)
        assertEquals("Morphology ultrasound", r.title)
        assertEquals(ResultCategory.ULTRASOUND, r.category)
        assertEquals("us-morph", r.protocolKey)
        assertEquals(LocalDate.of(2026, 10, 9), r.date)
        assertEquals(
            listOf("Idade gestacional: 23 semanas e 2 dias", "BCF: 148 bpm", "Peso fetal estimado: 640 g", "Líquido amniótico: Normal"),
            r.values,
        )
    }

    @Test
    fun appointmentSlip() {
        val rows = """
            Clínica Bem Nascer
            Av. Paulista, 1000 - sala 52
            AGENDAMENTO CONFIRMADO
            Exame: TOTG 75g (curva glicêmica)
            Data: 25/10   Horário: 07h30
            Dra. Marina Lopes
            Comparecer em jejum de 8 horas. Chegar 15 min antes.
            Tel: (11) 3333-4444
        """.trimIndent().lines()
        val a = OcrParser.parseAppointment(rows, today)
        assertEquals(EventKind.LAB, a.kind)
        assertEquals("Glucose tolerance test", a.title)
        assertEquals("ogtt", a.protocolKey)
        assertEquals(LocalDate.of(2026, 10, 25), a.date)
        assertEquals(LocalTime.of(7, 30), a.time)
        assertEquals("Clínica Bem Nascer · Av. Paulista, 1000 - sala 52", a.place)
        assertEquals("Dra. Marina Lopes", a.professional)
        assertTrue(a.notes.contains("jejum"))
    }

    @Test
    fun whatsappMessage() {
        val rows = listOf(
            "Olá Ana! Sua consulta de pré-natal com a Dra. Marina está confirmada",
            "para 3 de novembro às 14:30.",
            "Consultório: Rua Augusta, 200",
        )
        val a = OcrParser.parseAppointment(rows, today)
        assertEquals(EventKind.APPOINTMENT, a.kind)
        assertEquals(LocalDate.of(2026, 11, 3), a.date)
        assertEquals(LocalTime.of(14, 30), a.time)
        assertEquals("Dra. Marina", a.professional)
    }

    @Test
    fun vaccineSlip() {
        val a = OcrParser.parseAppointment(listOf("UBS Vila Mariana", "Vacina dTpa - gestante", "Dia 31/10/2026 das 8h às 16h"), today)
        assertEquals(EventKind.VACCINE, a.kind)
        assertEquals("vaccine-dtpa", a.protocolKey)
        assertEquals(LocalDate.of(2026, 10, 31), a.date)
        assertEquals(LocalTime.of(8, 0), a.time)
    }

    @Test
    fun rowsJoinPiecesOnTheSameLine() {
        // OCR often returns the name and the value of a lab line as separate pieces.
        val rows = OcrParser.rows(
            listOf(
                OcrLine("12,1 g/dL", 600, 101, 760, 129),
                OcrLine("HEMOGLOBINA", 40, 100, 300, 130),
                OcrLine("HEMATÓCRITO", 40, 150, 300, 180),
                OcrLine("36,4 %", 600, 152, 700, 178),
            ),
        )
        assertEquals(listOf("HEMOGLOBINA  12,1 g/dL", "HEMATÓCRITO  36,4 %"), rows)
        assertEquals(listOf("Hemoglobina: 12,1 g/dL", "Hematócrito: 36,4 %"), OcrParser.values(rows))
    }

    @Test
    fun shortCodesOnlyMatchWholeWords() {
        // "áreas" contains "eas" (urinalysis) but must not be read as one.
        val r = OcrParser.parseResult(listOf("Áreas avaliadas sem alterações"), today)
        assertEquals("Lab results", r.title)
    }
}
