package com.prenatal.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

data class Profile(
    val motherName: String = "",
    val babyName: String = "",
    /** Due date (DPP). The single source of truth for the gestational age. */
    val dueDate: LocalDate? = null,
    val bloodType: String = "",
    val doctor: String = "",
    val doctorPhone: String = "",
    val hospital: String = "",
    val hospitalPhone: String = "",
    val notes: String = "",
)

enum class EventKind(val label: String) {
    APPOINTMENT("Appointment"),
    ULTRASOUND("Ultrasound"),
    LAB("Lab test"),
    VACCINE("Vaccine"),
    OTHER("Other"),
}

data class Event(
    val id: String = newId(),
    val kind: EventKind = EventKind.APPOINTMENT,
    val title: String = "",
    val at: LocalDateTime,
    val place: String = "",
    val professional: String = "",
    val notes: String = "",
    val done: Boolean = false,
    val remind: Boolean = true,
    /** The suggested-checkup item this event fulfils, if any. */
    val protocolKey: String? = null,
)

enum class ResultCategory(val label: String) { LAB("Lab"), ULTRASOUND("Ultrasound"), OTHER("Other") }

data class Attachment(val id: String = newId(), val name: String, val mime: String)

data class Result(
    val id: String = newId(),
    val title: String = "",
    val date: LocalDate,
    val category: ResultCategory = ResultCategory.LAB,
    val values: String = "",
    val notes: String = "",
    val attachments: List<Attachment> = emptyList(),
)

data class Measurement(
    val id: String = newId(),
    val date: LocalDate,
    val weightKg: Double? = null,
    val bpSystolic: Int? = null,
    val bpDiastolic: Int? = null,
    val fundalCm: Double? = null,
    val fetalHr: Int? = null,
    val notes: String = "",
)

data class CheckItem(val id: String = newId(), val text: String, val done: Boolean = false)

/** Everything the app stores, as one document. */
data class PregnancyData(
    val profile: Profile = Profile(),
    val events: List<Event> = emptyList(),
    val results: List<Result> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val bag: List<CheckItem> = defaultBag(),
    /** Suggested checkups marked "not needed" (e.g. anti-D when Rh positive). */
    val skippedProtocol: Set<String> = emptySet(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("version", 1)
        .put("profile", profile.toJson())
        .put("events", JSONArray(events.map { it.toJson() }))
        .put("results", JSONArray(results.map { it.toJson() }))
        .put("measurements", JSONArray(measurements.map { it.toJson() }))
        .put("bag", JSONArray(bag.map { JSONObject().put("id", it.id).put("text", it.text).put("done", it.done) }))
        .put("skippedProtocol", JSONArray(skippedProtocol.toList()))

    companion object {
        fun fromJson(o: JSONObject) = PregnancyData(
            profile = o.optJSONObject("profile")?.let(::profileFrom) ?: Profile(),
            events = o.optJSONArray("events").objects().map(::eventFrom),
            results = o.optJSONArray("results").objects().map(::resultFrom),
            measurements = o.optJSONArray("measurements").objects().map(::measurementFrom),
            bag = o.optJSONArray("bag")?.objects()?.map { CheckItem(it.getString("id"), it.getString("text"), it.optBoolean("done")) }
                ?: defaultBag(),
            skippedProtocol = o.optJSONArray("skippedProtocol")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty(),
        )
    }
}

// ---------- JSON helpers ----------

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
private fun JSONObject.str(k: String) = optString(k, "")
private fun JSONObject.dbl(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null
private fun JSONObject.int(k: String): Int? = if (has(k) && !isNull(k)) optInt(k) else null
private fun JSONObject.putOpt2(k: String, v: Any?): JSONObject = if (v == null) this else put(k, v)

private fun Profile.toJson() = JSONObject()
    .put("motherName", motherName).put("babyName", babyName).putOpt2("dueDate", dueDate?.toString())
    .put("bloodType", bloodType).put("doctor", doctor).put("doctorPhone", doctorPhone)
    .put("hospital", hospital).put("hospitalPhone", hospitalPhone).put("notes", notes)

private fun profileFrom(o: JSONObject) = Profile(
    o.str("motherName"), o.str("babyName"), o.str("dueDate").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
    o.str("bloodType"), o.str("doctor"), o.str("doctorPhone"), o.str("hospital"), o.str("hospitalPhone"), o.str("notes"),
)

private fun Event.toJson() = JSONObject()
    .put("id", id).put("kind", kind.name).put("title", title).put("at", at.toString()).put("place", place)
    .put("professional", professional).put("notes", notes).put("done", done).put("remind", remind)
    .putOpt2("protocolKey", protocolKey)

private fun eventFrom(o: JSONObject) = Event(
    id = o.getString("id"),
    kind = runCatching { EventKind.valueOf(o.str("kind")) }.getOrDefault(EventKind.OTHER),
    title = o.str("title"),
    at = LocalDateTime.parse(o.getString("at")),
    place = o.str("place"),
    professional = o.str("professional"),
    notes = o.str("notes"),
    done = o.optBoolean("done"),
    remind = o.optBoolean("remind", true),
    protocolKey = o.str("protocolKey").takeIf { it.isNotEmpty() },
)

private fun Result.toJson() = JSONObject()
    .put("id", id).put("title", title).put("date", date.toString()).put("category", category.name)
    .put("values", values).put("notes", notes)
    .put("attachments", JSONArray(attachments.map { JSONObject().put("id", it.id).put("name", it.name).put("mime", it.mime) }))

private fun resultFrom(o: JSONObject) = Result(
    id = o.getString("id"),
    title = o.str("title"),
    date = LocalDate.parse(o.getString("date")),
    category = runCatching { ResultCategory.valueOf(o.str("category")) }.getOrDefault(ResultCategory.OTHER),
    values = o.str("values"),
    notes = o.str("notes"),
    attachments = o.optJSONArray("attachments").objects().map { Attachment(it.getString("id"), it.str("name"), it.str("mime")) },
)

private fun Measurement.toJson() = JSONObject()
    .put("id", id).put("date", date.toString()).putOpt2("weightKg", weightKg).putOpt2("bpSystolic", bpSystolic)
    .putOpt2("bpDiastolic", bpDiastolic).putOpt2("fundalCm", fundalCm).putOpt2("fetalHr", fetalHr).put("notes", notes)

private fun measurementFrom(o: JSONObject) = Measurement(
    id = o.getString("id"),
    date = LocalDate.parse(o.getString("date")),
    weightKg = o.dbl("weightKg"),
    bpSystolic = o.int("bpSystolic"),
    bpDiastolic = o.int("bpDiastolic"),
    fundalCm = o.dbl("fundalCm"),
    fetalHr = o.int("fetalHr"),
    notes = o.str("notes"),
)

/** A starting hospital bag list (mala da maternidade), fully editable. */
fun defaultBag(): List<CheckItem> = listOf(
    "Documents: ID (RG), CPF, health card (SUS / plano)",
    "Prenatal booklet (caderneta da gestante) and recent exams",
    "Comfortable nightgowns that open at the front (2–3)",
    "Nursing bras and breast pads",
    "Maternity pads and comfortable underwear",
    "Toiletries, hair ties, lip balm",
    "Slippers and a robe",
    "Phone charger",
    "Baby: bodysuits and footed pajamas (one per day + extra)",
    "Baby: hat, socks, mittens and a swaddle blanket",
    "Baby: diapers (newborn size) and wipes",
    "Baby: going-home outfit",
    "Car seat installed for the ride home",
).map { CheckItem(text = it) }
