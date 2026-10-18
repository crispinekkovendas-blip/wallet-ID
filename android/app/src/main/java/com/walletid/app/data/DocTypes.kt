package com.walletid.app.data

/** How a field is edited and shown. */
enum class FieldKind { TEXT, DATE, CPF, SELECT, MULTILINE }

data class FieldDef(
    val key: String,
    val label: String,
    val kind: FieldKind = FieldKind.TEXT,
    val options: List<String> = emptyList(),
    /** Shown as the big number on the card. */
    val primary: Boolean = false,
    /** Masked until the user taps Reveal. */
    val secret: Boolean = false,
    val mono: Boolean = false,
    val placeholder: String = "",
    val required: Boolean = false,
)

data class DocType(
    val key: String,
    val abbr: String,
    val name: String,
    val sub: String,
    val c1: Long,
    val c2: Long,
    val ink: Long,
    val gold: Boolean = false,
    val fields: List<FieldDef>,
)

/**
 * Document catalogue. Keys and field names match the web app (types.js) so
 * encrypted backups move freely between the two.
 */
object DocTypes {
    private val UFS = listOf(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
        "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
    )

    private val holder = FieldDef("holder", "Full name")
    private val cpf = FieldDef("cpf", "CPF", FieldKind.CPF, secret = true, mono = true)
    private val birth = FieldDef("birth", "Date of birth", FieldKind.DATE)
    private val issued = FieldDef("issued", "Issue date", FieldKind.DATE)
    private val expires = FieldDef("expires", "Valid until", FieldKind.DATE)
    private val uf = FieldDef("uf", "State (UF)", FieldKind.SELECT, options = listOf("") + UFS)
    private val birthplace = FieldDef("birthplace", "Place of birth")

    val all: List<DocType> = listOf(
        DocType(
            "rg", "RG", "Registro Geral", "Carteira de Identidade", 0xFF0F5C4A, 0xFF2A9D7C, 0xFFEAFFF7,
            fields = listOf(
                holder,
                FieldDef("number", "RG number", primary = true, secret = true, mono = true),
                FieldDef("issuer", "Issuing body", placeholder = "SSP"),
                uf, cpf, birth, issued, birthplace,
                FieldDef("parents", "Parents (filiação)", FieldKind.MULTILINE),
            ),
        ),
        DocType(
            "cin", "CIN", "Carteira de Identidade Nacional", "Nova identidade", 0xFF0B3D91, 0xFF1D74D8, 0xFFEEF5FF,
            fields = listOf(holder, cpf.copy(primary = true), birth, issued, expires, birthplace),
        ),
        DocType(
            "cnh", "CNH", "Carteira Nacional de Habilitação", "Driver’s license", 0xFF1D5A2C, 0xFF7FB238, 0xFFF4FFE9,
            fields = listOf(
                holder,
                FieldDef("number", "Registration no. (Nº registro)", primary = true, secret = true, mono = true),
                FieldDef("category", "Category", FieldKind.SELECT, listOf("", "A", "B", "AB", "C", "D", "E", "AC", "AD", "AE")),
                cpf, expires,
                FieldDef("firstLicense", "First license (1ª habilitação)", FieldKind.DATE),
                issued,
                FieldDef("issuer", "Issuer", placeholder = "DETRAN-SP"),
            ),
        ),
        DocType(
            "cpf", "CPF", "Cadastro de Pessoas Físicas", "Receita Federal", 0xFF14284B, 0xFF2F5597, 0xFFEEF3FF,
            fields = listOf(holder, cpf.copy(key = "number", primary = true), birth),
        ),
        DocType(
            "creci", "CRECI", "Conselho Regional de Corretores de Imóveis", "Real estate broker",
            0xFF151A2E, 0xFF3A3F63, 0xFFF6ECD2, gold = true,
            fields = listOf(
                holder,
                FieldDef("number", "Registration no. (Inscrição)", primary = true, mono = true),
                FieldDef("region", "Region", placeholder = "CRECI-SP · 2ª Região"),
                FieldDef("kind", "Registration type", FieldKind.SELECT, listOf("", "Pessoa Física", "Pessoa Jurídica", "Estagiário")),
                cpf, issued, expires,
            ),
        ),
        DocType(
            "council", "CONSELHO", "Conselho profissional", "OAB · CRM · CREA · CRC…", 0xFF3B1D4F, 0xFF7A3E9D, 0xFFF8EEFF,
            fields = listOf(
                holder,
                FieldDef(
                    "council", "Council", FieldKind.SELECT,
                    listOf("", "OAB", "CRM", "CREA", "CRC", "CRO", "CRF", "COREN", "CRP", "CAU", "CREFITO", "CRN", "Outro"),
                ),
                FieldDef("number", "Registration number", primary = true, mono = true),
                uf, cpf, issued, expires,
            ),
        ),
        DocType(
            "titulo", "TÍTULO", "Título de Eleitor", "Justiça Eleitoral", 0xFF5A1A1A, 0xFFA8432F, 0xFFFFF1EC,
            fields = listOf(
                holder,
                FieldDef("number", "Voter registration no.", primary = true, mono = true),
                FieldDef("zone", "Zone (zona)"),
                FieldDef("section", "Section (seção)"),
                FieldDef("city", "City"), uf, issued,
            ),
        ),
        DocType(
            "passport", "PASSAPORTE", "República Federativa do Brasil", "Passport",
            0xFF0E1F3D, 0xFF1F3D73, 0xFFF3EFE0, gold = true,
            fields = listOf(
                holder,
                FieldDef("number", "Passport number", primary = true, secret = true, mono = true),
                FieldDef("country", "Issuing country", placeholder = "Brasil"),
                FieldDef("nationality", "Nationality"),
                birth, issued, expires,
            ),
        ),
        DocType(
            "ctps", "CTPS", "Carteira de Trabalho", "Digital ou física", 0xFF173B5C, 0xFF2D6E9E, 0xFFEEF7FF,
            fields = listOf(
                holder,
                FieldDef("number", "CTPS number", primary = true, mono = true),
                FieldDef("series", "Series (série)"), uf,
                FieldDef("pis", "PIS / NIS", mono = true), cpf,
            ),
        ),
        DocType(
            "pis", "PIS/NIS", "PIS · PASEP · NIS", "Número social", 0xFF164A43, 0xFF2F8077, 0xFFECFFFB,
            fields = listOf(holder, FieldDef("number", "Number", primary = true, mono = true), cpf),
        ),
        DocType(
            "sus", "SUS", "Cartão Nacional de Saúde", "CNS", 0xFF08506B, 0xFF1A97B8, 0xFFEAFAFF,
            fields = listOf(holder, FieldDef("number", "CNS number", primary = true, mono = true), cpf, birth),
        ),
        DocType(
            "health", "SAÚDE", "Plano de saúde", "Health insurance", 0xFF7A1F4A, 0xFFC74A7D, 0xFFFFF0F6,
            fields = listOf(
                holder,
                FieldDef("provider", "Provider", placeholder = "Unimed, Amil, SulAmérica…"),
                FieldDef("number", "Member number (carteirinha)", primary = true, mono = true),
                FieldDef("plan", "Plan"), expires,
            ),
        ),
        DocType(
            "crlv", "CRLV", "Documento do veículo", "CRLV-e", 0xFF3D3D3D, 0xFF6F6F6F, 0xFFFFFFFF,
            fields = listOf(
                FieldDef("holder", "Owner"),
                FieldDef("plate", "Plate (placa)", primary = true, mono = true),
                FieldDef("renavam", "RENAVAM", mono = true),
                FieldDef("model", "Make / model"),
                FieldDef("year", "Year"), expires,
            ),
        ),
        DocType(
            "reservista", "RESERVISTA", "Certificado de Reservista", "Serviço militar", 0xFF3C4A24, 0xFF6B7D3A, 0xFFF6FBE9,
            fields = listOf(
                holder,
                FieldDef("number", "Certificate number", primary = true, mono = true),
                FieldDef("ra", "RA"), FieldDef("series", "Series"), cpf, issued,
            ),
        ),
        DocType(
            "certidao", "CERTIDÃO", "Certidão de registro civil", "Nascimento · Casamento", 0xFF5B4A2E, 0xFF9C7F4B, 0xFFFFF8EA,
            fields = listOf(
                holder,
                FieldDef("kind", "Type", FieldKind.SELECT, listOf("", "Nascimento", "Casamento", "Óbito")),
                FieldDef("number", "Registration (matrícula)", primary = true, mono = true),
                FieldDef("registry", "Registry office (cartório)"), issued,
            ),
        ),
        DocType(
            "other", "ID", "Other document", "Anything else", 0xFF1F2937, 0xFF4B5563, 0xFFF3F4F6,
            fields = listOf(
                FieldDef("title", "Document name", placeholder = "e.g. Student ID", required = true),
                holder,
                FieldDef("number", "Number", primary = true, mono = true),
                FieldDef("issuer", "Issuer"), issued, expires,
            ),
        ),
    )

    private val byKey = all.associateBy { it.key }

    // Free-text type names saved by the first web version.
    private val legacy = mapOf(
        "passport" to "passport", "driver's license" to "cnh", "national id" to "rg", "health insurance" to "health",
    )

    fun get(key: String?): DocType = byKey[key] ?: byKey.getValue("other")

    fun isKnown(key: String?) = key != null && byKey.containsKey(key)

    fun normalize(fields: Map<String, String>): Map<String, String> {
        if (isKnown(fields["type"])) return fields
        val mapped = legacy[(fields["type"] ?: "").lowercase()] ?: "other"
        val title = fields["title"].orEmpty().ifEmpty { fields["name"].orEmpty() }.ifEmpty { fields["type"].orEmpty() }
        return fields + mapOf("type" to mapped, "title" to title)
    }
}
