package com.prenatal.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Where the pregnancy is on a given day, worked out from the due date (40 weeks = 280 days). */
data class Gestation(val days: Int, val dueDate: LocalDate, val today: LocalDate) {
    val weeks: Int get() = days / 7
    val dayOfWeek: Int get() = days % 7
    val trimester: Int get() = when {
        weeks < 14 -> 1
        weeks < 28 -> 2
        else -> 3
    }
    val daysToGo: Long get() = ChronoUnit.DAYS.between(today, dueDate)
    val progress: Float get() = (days / 280f).coerceIn(0f, 1f)
    val label: String get() = "$weeks weeks" + if (dayOfWeek > 0) " $dayOfWeek day${if (dayOfWeek == 1) "" else "s"}" else ""
    val short: String get() = "${weeks}w${dayOfWeek}d"

    /** The date on which a given week begins. */
    fun dateOfWeek(week: Int): LocalDate = dueDate.minusDays(280L - week * 7L)

    companion object {
        fun fromDueDate(due: LocalDate, today: LocalDate = LocalDate.now()) =
            Gestation((280 - ChronoUnit.DAYS.between(today, due)).toInt().coerceAtLeast(0), due, today)

        /** Naegele's rule: due date = first day of the last period + 280 days. */
        fun dueDateFromLmp(lmp: LocalDate): LocalDate = lmp.plusDays(280)

        fun weeksAt(due: LocalDate, date: LocalDate): Int = ((280 - ChronoUnit.DAYS.between(date, due)) / 7).toInt()
    }
}

/** A friendly size comparison for each week. */
object BabySize {
    private val sizes = mapOf(
        4 to ("🌱" to "a poppy seed"), 5 to ("🌱" to "a sesame seed"), 6 to ("🫘" to "a lentil"),
        7 to ("🫐" to "a blueberry"), 8 to ("🍇" to "a raspberry"), 9 to ("🍒" to "a cherry"),
        10 to ("🍓" to "a strawberry"), 11 to ("🌰" to "a fig"), 12 to ("🍋" to "a lime"),
        13 to ("🫛" to "a pea pod"), 14 to ("🍋" to "a lemon"), 15 to ("🍎" to "an apple"),
        16 to ("🥑" to "an avocado"), 17 to ("🍐" to "a pear"), 18 to ("🫑" to "a bell pepper"),
        19 to ("🍅" to "a big tomato"), 20 to ("🍌" to "a banana"), 21 to ("🥕" to "a carrot"),
        22 to ("🥭" to "a papaya"), 23 to ("🍊" to "a grapefruit"), 24 to ("🌽" to "an ear of corn"),
        25 to ("🥦" to "a cauliflower"), 26 to ("🥬" to "a head of lettuce"), 27 to ("🥦" to "a broccoli head"),
        28 to ("🍆" to "an eggplant"), 29 to ("🎃" to "a butternut squash"), 30 to ("🥬" to "a cabbage"),
        31 to ("🥥" to "a coconut"), 32 to ("🥒" to "a squash"), 33 to ("🍍" to "a pineapple"),
        34 to ("🍈" to "a cantaloupe"), 35 to ("🍈" to "a honeydew melon"), 36 to ("🥬" to "a romaine lettuce"),
        37 to ("🥬" to "a bunch of chard"), 38 to ("🎃" to "a small pumpkin"), 39 to ("🍉" to "a small watermelon"),
        40 to ("🍉" to "a watermelon"),
    )

    fun forWeek(week: Int): Pair<String, String>? = sizes[week.coerceAtMost(40)].takeIf { week >= 4 }
}

/**
 * Suggested checkups, a general guide based on the Brazilian Ministry of Health prenatal
 * recommendations. The doctor's own plan always comes first.
 */
data class ProtocolItem(
    val key: String,
    val title: String,
    val local: String,
    val fromWeek: Int,
    val toWeek: Int,
    val kind: EventKind,
    val details: String,
)

enum class ProtocolStatus { DONE, SCHEDULED, DUE_NOW, UPCOMING, OVERDUE, SKIPPED }

object Protocol {
    val items = listOf(
        ProtocolItem(
            "first-visit", "First prenatal visit", "1ª consulta de pré-natal", 4, 12, EventKind.APPOINTMENT,
            "Ideally before 12 weeks: history, exam, and the request for the first lab tests.",
        ),
        ProtocolItem(
            "labs-1", "1st trimester lab tests", "Exames do 1º trimestre", 4, 13, EventKind.LAB,
            "Blood type and Rh, complete blood count, fasting glucose, HIV, syphilis (VDRL), hepatitis B and C, " +
                "toxoplasmosis, rubella, urinalysis and urine culture, TSH.",
        ),
        ProtocolItem(
            "us-early", "Early ultrasound", "Ultrassom obstétrico inicial", 6, 11, EventKind.ULTRASOUND,
            "Confirms the pregnancy and dates it.",
        ),
        ProtocolItem(
            "us-nt", "Nuchal translucency ultrasound", "Ultrassom morfológico do 1º trimestre (TN)", 11, 14, EventKind.ULTRASOUND,
            "Between 11 and 14 weeks: early anatomy and screening for chromosomal conditions.",
        ),
        ProtocolItem(
            "us-morph", "Morphology ultrasound", "Ultrassom morfológico do 2º trimestre", 20, 24, EventKind.ULTRASOUND,
            "Detailed look at the baby's anatomy, placenta and amniotic fluid.",
        ),
        ProtocolItem(
            "vaccine-dtpa", "Whooping cough vaccine (dTpa)", "Vacina dTpa", 20, 36, EventKind.VACCINE,
            "From 20 weeks, in every pregnancy. Protects the baby in the first months.",
        ),
        ProtocolItem(
            "vaccine-flu", "Flu vaccine", "Vacina da gripe (influenza)", 4, 40, EventKind.VACCINE,
            "Any trimester, during the vaccination campaign.",
        ),
        ProtocolItem(
            "vaccine-hepb", "Hepatitis B vaccine", "Vacina hepatite B", 4, 40, EventKind.VACCINE,
            "If not already vaccinated (check your vaccination card).",
        ),
        ProtocolItem(
            "ogtt", "Glucose tolerance test", "TOTG 75g (curva glicêmica)", 24, 28, EventKind.LAB,
            "Screens for gestational diabetes.",
        ),
        ProtocolItem(
            "anti-d", "Anti-D immunoglobulin (Rh negative only)", "Imunoglobulina anti-D", 28, 30, EventKind.VACCINE,
            "Only if the mother is Rh negative and the father is Rh positive or unknown.",
        ),
        ProtocolItem(
            "labs-3", "3rd trimester lab tests", "Exames do 3º trimestre", 28, 32, EventKind.LAB,
            "Repeat blood count, glucose, HIV, syphilis, hepatitis B, toxoplasmosis (if not immune), urinalysis and culture.",
        ),
        ProtocolItem(
            "us-growth", "Growth and Doppler ultrasound", "Ultrassom com Doppler", 28, 36, EventKind.ULTRASOUND,
            "Checks the baby's growth and blood flow, when your doctor asks for it.",
        ),
        ProtocolItem(
            "gbs", "Group B strep swab", "Cultura para estreptococo do grupo B", 35, 37, EventKind.LAB,
            "A quick swab between 35 and 37 weeks.",
        ),
        ProtocolItem(
            "ctg", "Fetal monitoring (CTG)", "Cardiotocografia", 37, 41, EventKind.LAB,
            "From 37 weeks, when your doctor asks for it.",
        ),
    )

    fun get(key: String?) = items.firstOrNull { it.key == key }

    /** Visit frequency: monthly to 28 weeks, every two weeks to 36, then weekly. */
    fun visitRhythm(week: Int) = when {
        week < 28 -> "Visits are usually monthly until 28 weeks"
        week < 36 -> "Visits are usually every two weeks until 36 weeks"
        else -> "Visits are usually weekly from 36 weeks"
    }

    fun status(item: ProtocolItem, week: Int, events: List<Event>, skipped: Set<String>): ProtocolStatus {
        val linked = events.filter { it.protocolKey == item.key }
        return when {
            item.key in skipped -> ProtocolStatus.SKIPPED
            linked.any { it.done } -> ProtocolStatus.DONE
            linked.isNotEmpty() -> ProtocolStatus.SCHEDULED
            week > item.toWeek -> ProtocolStatus.OVERDUE
            week >= item.fromWeek -> ProtocolStatus.DUE_NOW
            else -> ProtocolStatus.UPCOMING
        }
    }
}
