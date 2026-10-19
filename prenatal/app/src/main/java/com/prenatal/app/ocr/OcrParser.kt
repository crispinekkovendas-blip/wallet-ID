package com.prenatal.app.ocr

import com.prenatal.app.data.EventKind
import com.prenatal.app.data.ResultCategory
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay

/** What a photo was read as, plus the raw text so nothing the parser missed is lost. */
data class Scan<T>(val draft: T, val text: String, val stamp: Long = System.nanoTime())

/** A piece of recognised text and where it sits on the page. */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

data class ScannedResult(
    val title: String,
    val category: ResultCategory,
    val date: LocalDate?,
    val values: List<String>,
    val protocolKey: String?,
)

data class ScannedAppointment(
    val kind: EventKind,
    val title: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val place: String,
    val professional: String,
    val notes: String,
    val protocolKey: String?,
)

/**
 * Turns OCR text from Brazilian lab reports, ultrasound reports and appointment slips into
 * structured drafts. Everything here is heuristic: the user always reviews before saving.
 */
object OcrParser {

    // ---------- Layout ----------

    /**
     * Rebuilds the page's rows. Reports put the test name on the left and the value on the
     * right, which OCR often returns as separate pieces; joining pieces that share a row
     * gives "HEMOGLOBINA 12,1 g/dL" again.
     */
    fun rows(lines: List<OcrLine>): List<String> {
        if (lines.isEmpty()) return emptyList()
        val sorted = lines.sortedBy { (it.top + it.bottom) / 2 }
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in sorted) {
            val cy = (line.top + line.bottom) / 2
            val h = (line.bottom - line.top).coerceAtLeast(1)
            val row = rows.lastOrNull()
            val rowCy = row?.let { r -> r.sumOf { (it.top + it.bottom) / 2 } / r.size }
            if (row != null && rowCy != null && kotlin.math.abs(cy - rowCy) <= h * 0.55) row.add(line) else rows.add(mutableListOf(line))
        }
        return rows.map { r -> r.sortedBy { it.left }.joinToString("  ") { it.text.trim() } }.filter { it.isNotBlank() }
    }

    // ---------- Shared helpers ----------

    /** Lowercase without accents, for keyword matching. */
    fun plain(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private val MONTHS = listOf(
        "janeiro", "fevereiro", "marco", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )

    private val FULL_DATE = Regex("""\b(\d{1,2})\s?[/.-]\s?(\d{1,2})\s?[/.-]\s?(\d{2,4})\b""")
    private val SHORT_DATE = Regex("""(?<![\d/])(\d{1,2})/(\d{1,2})(?![\d/])""")
    private val WORD_DATE = Regex("""\b(\d{1,2})\s+de\s+(janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)(?:\s+de\s+(\d{4}))?""")
    private val TIME = Regex("""(?<![\d/.,])([01]?\d|2[0-3])\s?(?::|h|hs|hrs)\s?([0-5]\d)?(?![\d/])""", RegexOption.IGNORE_CASE)

    private fun fullDate(m: MatchResult): LocalDate? {
        val (d, mo, y) = m.destructured
        val year = y.toInt().let { if (it < 100) 2000 + it else it }
        return runCatching { LocalDate.of(year, mo.toInt(), d.toInt()) }.getOrNull()?.takeIf { it.year in 2000..2100 }
    }

    /** Dates written anywhere in a row: 12/10/2026, 12/10, or 12 de outubro (de 2026). */
    fun datesIn(row: String, today: LocalDate): List<LocalDate> {
        val p = plain(row)
        val out = mutableListOf<LocalDate>()
        FULL_DATE.findAll(p).forEach { m -> fullDate(m)?.let(out::add) }
        if (out.isEmpty()) {
            WORD_DATE.findAll(p).forEach { m ->
                val (d, month, y) = m.destructured
                val mo = MONTHS.indexOf(month) + 1
                val date = if (y.isNotEmpty()) runCatching { LocalDate.of(y.toInt(), mo, d.toInt()) }.getOrNull() else nextOccurrence(d.toInt(), mo, today)
                date?.let(out::add)
            }
        }
        if (out.isEmpty()) {
            SHORT_DATE.findAll(p).forEach { m ->
                val (d, mo) = m.destructured
                nextOccurrence(d.toInt(), mo.toInt(), today)?.let(out::add)
            }
        }
        return out
    }

    private fun nextOccurrence(day: Int, month: Int, today: LocalDate): LocalDate? {
        val md = runCatching { MonthDay.of(month, day) }.getOrNull() ?: return null
        val thisYear = md.atYear(today.year)
        return if (thisYear.isBefore(today.minusDays(7))) md.atYear(today.year + 1) else thisYear
    }

    fun timeIn(row: String): LocalTime? {
        // Ignore times inside dates or phone numbers by looking at the row without them.
        val cleaned = FULL_DATE.replace(row, " ")
        val times = TIME.findAll(cleaned)
            .filter { it.groupValues[2].isNotEmpty() || it.value.contains('h', ignoreCase = true) }
            .map { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].ifEmpty { "0" }.toInt()) }
            .toList()
        // "Chegar 1h antes" is not the appointment time; prefer daytime hours.
        return times.firstOrNull { it.hour in 6..21 } ?: times.firstOrNull()
    }

    /** Known exams and vaccines: keyword (plain text) → display title, kind, result category, suggested-checkup key. */
    private data class Known(val keys: List<String>, val title: String, val kind: EventKind, val category: ResultCategory, val protocol: String?)

    private val KNOWN = listOf(
        Known(listOf("translucencia nucal", "morfologico do 1", "morfologico de 1", " tn "), "Nuchal translucency ultrasound", EventKind.ULTRASOUND, ResultCategory.ULTRASOUND, "us-nt"),
        Known(listOf("morfologic"), "Morphology ultrasound", EventKind.ULTRASOUND, ResultCategory.ULTRASOUND, "us-morph"),
        Known(listOf("doppler"), "Doppler ultrasound", EventKind.ULTRASOUND, ResultCategory.ULTRASOUND, "us-growth"),
        Known(listOf("ultrassom", "ultrassonografia", "ultra-som", "ecografia", "usg", "obstetrica"), "Ultrasound", EventKind.ULTRASOUND, ResultCategory.ULTRASOUND, null),
        Known(listOf("totg", "curva glicemica", "tolerancia a glicose", "tolerancia oral"), "Glucose tolerance test", EventKind.LAB, ResultCategory.LAB, "ogtt"),
        Known(listOf("estreptococo", "streptococcus", "strepto b", "gbs"), "Group B strep swab", EventKind.LAB, ResultCategory.LAB, "gbs"),
        Known(listOf("cardiotoco", "ctg"), "Fetal monitoring (CTG)", EventKind.LAB, ResultCategory.LAB, "ctg"),
        Known(listOf("dtpa", "coqueluche"), "Whooping cough vaccine (dTpa)", EventKind.VACCINE, ResultCategory.OTHER, "vaccine-dtpa"),
        Known(listOf("influenza", "gripe"), "Flu vaccine", EventKind.VACCINE, ResultCategory.OTHER, "vaccine-flu"),
        Known(listOf("vacina hepatite b", "vacina contra hepatite b"), "Hepatitis B vaccine", EventKind.VACCINE, ResultCategory.OTHER, "vaccine-hepb"),
        Known(listOf("anti-d", "anti d", "imunoglobulina"), "Anti-D immunoglobulin", EventKind.VACCINE, ResultCategory.OTHER, "anti-d"),
        Known(listOf("vacina"), "Vaccine", EventKind.VACCINE, ResultCategory.OTHER, null),
        Known(listOf("hemograma"), "Blood count (hemograma)", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("glicemia", "glicose"), "Fasting glucose", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("urocultura"), "Urine culture", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("urina", "eas", "urinalise", "sumario de urina"), "Urinalysis", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("tipagem", "grupo sanguineo", "fator rh"), "Blood type and Rh", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("toxoplasm"), "Toxoplasmosis", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("rubeola"), "Rubella", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("vdrl", "sifilis", "treponem"), "Syphilis (VDRL)", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("hiv"), "HIV", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("hbsag", "hepatite b"), "Hepatitis B", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("hepatite c", "anti-hcv", "anti hcv"), "Hepatitis C", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("tsh", "tireo"), "Thyroid (TSH)", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("coleta", "exame de sangue", "laboratorio", "exames laboratoriais"), "Lab tests", EventKind.LAB, ResultCategory.LAB, null),
        Known(listOf("pre-natal", "prenatal", "pre natal", "consulta", "obstetra", "ginecolog"), "Prenatal visit", EventKind.APPOINTMENT, ResultCategory.OTHER, null),
    )

    private fun findKnown(text: String, kinds: Set<EventKind>? = null): Known? {
        val p = " " + plain(text).replace(Regex("[^a-z0-9-]+"), " ") + " "
        // Short codes (EAS, HIV, TSH…) must be whole words: "eas" is also inside "áreas".
        return KNOWN.firstOrNull { k ->
            (kinds == null || k.kind in kinds) && k.keys.any { key -> if (key.trim().length <= 4) p.contains(" ${key.trim()} ") else p.contains(key) }
        }
    }

    // ---------- Results ----------

    private val NUMBER = """[<>≤≥]?\s?\d+(?:[.,]\d+)*"""
    // \p{L} rather than \w so accented units ("milhões/mm³") stay whole; "/mm³" starts with a slash.
    private val UNIT = """/?[\p{L}µμ%][\p{L}\d/%³²µμ.^]{0,13}"""
    private val NUMERIC_ROW = Regex("""^(?<name>\p{L}[\p{L}\s().,'/+-]{1,60}?)\s*[:.…_–-]*\s+(?<value>$NUMBER)\s*(?<unit>$UNIT)?(?<rest>.*)$""")
    private val QUALITATIVE_ROW = Regex(
        """^(?<name>\p{L}[\p{L}\s()0-9.,'/+-]{1,60}?)\s*[:.…_–-]*\s+(?<value>n[ãa]o\s+reagente|reagente|negativ[oa]|positiv[oa]|normal|ausentes?|presentes?|raros|raras|indetect[áa]vel|detect[áa]vel|imune|n[ãa]o\s+imune|suscet[íi]vel)\b(?<rest>.*)$""",
        RegexOption.IGNORE_CASE,
    )
    private val RANGE = Regex("""(\d+(?:[.,]\d+)?)\s*(?:a|até|ate|-|–)\s*(\d+(?:[.,]\d+)?)""")

    private val SKIP_NAMES = listOf(
        "data", "paciente", "nome", "medico", "crm", "pagina", "cadastro", "protocolo", "telefone", "tel", "fone", "cpf", "rg", "cep",
        "convenio", "prontuario", "pedido", "liberado", "assinatura", "emissao", "impressao", "horario", "hora", "endereco", "rua",
        "av", "avenida", "numero", "registro", "codigo", "os", "atendimento", "solicitante", "resultado anterior", "metodo",
        "material", "valores de referencia", "referencia", "unidade", "laudo", "nascimento", "dn", "dum", "dpp",
    )
    /** Skipped only when they are the whole label ("Idade: 32 anos"), not "Idade gestacional". */
    private val SKIP_EXACT = listOf("idade", "sexo")

    private fun tidyName(name: String): String {
        val n = name.trim().trimEnd('.', ':', '-', '–', '_', ' ').replace(Regex("\\s{2,}"), " ")
        val letters = n.filter { it.isLetter() }
        // ALL CAPS lab names read better as "Hemoglobina"; short acronyms (VDRL, BCF, TSH) stay as they are.
        return if (letters.length > 4 && letters.all { it.isUpperCase() }) n.lowercase().replaceFirstChar { it.uppercase() } else n
    }

    private fun skip(name: String): Boolean {
        val p = plain(name).trim().trimEnd('.', ':')
        return SKIP_NAMES.any { p == it || p.startsWith("$it ") || p.startsWith("$it:") } || p in SKIP_EXACT || p.length < 2
    }

    /** "Name: value unit (ref a–b)" lines from a lab or ultrasound report. */
    fun values(rows: List<String>): List<String> {
        val out = LinkedHashMap<String, String>()
        for (raw in rows) {
            val row = raw.replace('\t', ' ').trim()
            QUALITATIVE_ROW.matchEntire(row)?.let { m ->
                val name = tidyName(m.groups["name"]!!.value)
                if (!skip(name)) out.putIfAbsent(plain(name), "$name: ${m.groups["value"]!!.value.lowercase().replaceFirstChar { it.uppercase() }}")
                return@let
            } ?: NUMERIC_ROW.matchEntire(row)?.let { m ->
                val name = tidyName(m.groups["name"]!!.value)
                val rest = m.groups["rest"]?.value.orEmpty()
                val unit = m.groups["unit"]?.value.orEmpty()
                // Headings like "…MORFOLÓGICA DO 2º TRIMESTRE" and dates are not values.
                val heading = name.split(Regex("\\s+")).size > 5 || unit.startsWith("º") || unit.startsWith("ª") || plain(rest).contains("trimestre")
                if (skip(name) || heading || Regex("""^\s*/\d""").containsMatchIn(rest)) return@let
                val value = m.groups["value"]!!.value.replace(" ", "")
                // "23 semanas e 2 dias" keeps its days.
                val days = Regex("""^\s*e\s+(\d+)\s+dias?""", RegexOption.IGNORE_CASE).find(rest)?.let { " e ${it.groupValues[1]} dias" }.orEmpty()
                val ref = RANGE.find(rest)?.let { r -> " (ref ${r.groupValues[1]}–${r.groupValues[2]})" }.orEmpty()
                out.putIfAbsent(plain(name), "$name: $value${if (unit.isNotEmpty()) " $unit" else ""}$days$ref")
            }
        }
        return out.values.toList()
    }

    /** The collection or exam date: prefer a row that says so, never a birth date. */
    fun reportDate(rows: List<String>, today: LocalDate): LocalDate? {
        val candidates = rows.filterNot { plain(it).contains("nasc") }
        val preferred = candidates.filter { r -> plain(r).let { it.contains("coleta") || it.contains("realiz") || it.contains("data do exame") || it.contains("data:") } }
        return (preferred + candidates).asSequence()
            .flatMap { r -> FULL_DATE.findAll(plain(r)).mapNotNull(::fullDate) }
            .firstOrNull { !it.isAfter(today) }
    }

    fun parseResult(rows: List<String>, today: LocalDate = LocalDate.now()): ScannedResult {
        val text = rows.joinToString("\n")
        val known = findKnown(text, setOf(EventKind.LAB, EventKind.ULTRASOUND))
        return ScannedResult(
            title = known?.title ?: "Lab results",
            category = known?.category ?: ResultCategory.LAB,
            date = reportDate(rows, today),
            values = values(rows),
            protocolKey = known?.protocol,
        )
    }

    // ---------- Appointments ----------

    private val PLACE_WORDS = listOf("clinica", "hospital", "laboratorio", "maternidade", "ubs", "unidade", "centro", "consultorio", "posto", "lab ")
    private val ADDRESS_WORDS = listOf("rua ", "r. ", "av. ", "av ", "avenida", "alameda", "travessa", "rodovia", "estrada", "praca")
    private val PREP_WORDS = listOf("jejum", "preparo", "levar", "trazer", "bexiga", "chegar", "antecedencia", "documento", "pedido medico")

    fun parseAppointment(rows: List<String>, today: LocalDate = LocalDate.now()): ScannedAppointment {
        val text = rows.joinToString("\n")
        val known = findKnown(text)
        val dates = rows.filterNot { plain(it).contains("nasc") }.flatMap { datesIn(it, today) }
        val date = dates.firstOrNull { !it.isBefore(today) } ?: dates.firstOrNull()
        val time = rows.firstNotNullOfOrNull { timeIn(it) }
        val place = rows.firstOrNull { r -> plain(r).let { p -> PLACE_WORDS.any { p.contains(it) } && !p.contains("pedido") } }?.trim().orEmpty()
        val address = rows.firstOrNull { r -> plain(r).let { p -> ADDRESS_WORDS.any { p.startsWith(it) || p.contains(" $it") } } }?.trim().orEmpty()
        // Just the name ("Dra. Marina Lopes"), not the whole sentence around it.
        val doctor = Regex("""\b(?:Dra?|DRA?|Doutora?)\.?\s+\p{Lu}\p{L}+(?:\s+(?:de\s+|da\s+|do\s+)?\p{Lu}\p{L}+){0,3}""")
        val professional = rows.firstNotNullOfOrNull { doctor.find(it)?.value }?.trim().orEmpty()
        val prep = rows.filter { r -> plain(r).let { p -> PREP_WORDS.any { p.contains(it) } } }.map { it.trim() }
        return ScannedAppointment(
            kind = known?.kind ?: EventKind.APPOINTMENT,
            title = known?.title ?: "Appointment",
            date = date,
            time = time,
            place = listOf(place, address).filter { it.isNotEmpty() }.distinct().joinToString(" · "),
            professional = professional,
            notes = prep.joinToString("\n"),
            protocolKey = known?.protocol,
        )
    }
}
