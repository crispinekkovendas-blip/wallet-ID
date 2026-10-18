package com.walletid.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** One stored document. `fields` always contains "type" plus the type's field values. */
data class Doc(val id: String, val fields: Map<String, String>) {
    val type: DocType get() = DocTypes.get(fields["type"])
    operator fun get(key: String): String = fields[key].orEmpty()

    val title: String
        get() = get("title").ifEmpty {
            when (type.key) {
                "other" -> "Document"
                "council" -> get("council").ifEmpty { type.abbr }
                else -> type.abbr
            }
        }

    val primaryField: FieldDef? get() = type.fields.firstOrNull { it.primary }
    val primaryValue: String get() = primaryField?.let { get(it.key) }.orEmpty()

    val expiry: Expiry? get() = Expiry.of(get("expires"))

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return (listOf(title, type.name) + fields.values).any { it.lowercase().contains(q) }
    }
}

enum class ExpiryLevel { WARN, BAD }

data class Expiry(val level: ExpiryLevel, val days: Long) {
    val text: String
        get() = when {
            level == ExpiryLevel.BAD -> "Expired"
            days == 0L -> "Expires today"
            else -> "Expires in $days days"
        }

    companion object {
        const val SOON_DAYS = 90L

        fun of(isoDate: String, today: LocalDate = LocalDate.now()): Expiry? {
            val date = parseIso(isoDate) ?: return null
            val days = ChronoUnit.DAYS.between(today, date)
            return when {
                days < 0 -> Expiry(ExpiryLevel.BAD, days)
                days <= SOON_DAYS -> Expiry(ExpiryLevel.WARN, days)
                else -> null
            }
        }
    }
}

private val BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val BR_MONTH = DateTimeFormatter.ofPattern("MM/yy")

fun parseIso(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()
fun formatBrDate(iso: String): String = parseIso(iso)?.format(BR_DATE) ?: iso
fun formatBrMonth(iso: String): String = parseIso(iso)?.format(BR_MONTH) ?: iso

/** Keeps the last 3 letters/digits visible and hides the rest, leaving punctuation in place. */
fun mask(value: String): String {
    var keep = 3
    val out = StringBuilder()
    for (ch in value.reversed()) {
        out.append(if (ch.isLetterOrDigit()) (if (keep-- > 0) ch else '•') else ch)
    }
    return out.reverse().toString()
}

/** Formats up to 11 digits as 000.000.000-00 while typing. */
fun formatCpf(input: String): String {
    val d = input.filter(Char::isDigit).take(11)
    val sb = StringBuilder()
    d.forEachIndexed { i, c ->
        when (i) {
            3, 6 -> sb.append('.')
            9 -> sb.append('-')
        }
        sb.append(c)
    }
    return sb.toString()
}
