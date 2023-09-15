package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe

fun byggTiltakstypeFilter(tiltakstyper: List<String>): String {
    if (tiltakstyper.isEmpty()) return ""

    return """
            && tiltakstype->_id in ${tiltakstyper.toSanityListe()}
    """.trimIndent()
}

fun byggSokeFilter(sokestreng: String): String {
    if (sokestreng.isBlank()) return ""

    return """
            && [tiltaksgjennomforingNavn, string(tiltaksnummer.current), tiltakstype->tiltakstypeNavn, lokasjon, oppstartsdato] match "*$sokestreng*"
    """.trimIndent()
}

fun byggInnsatsgruppeFilter(innsatsgruppe: String?): String {
    if (innsatsgruppe.isNullOrBlank()) return ""

    return """
             && tiltakstype->innsatsgruppe->nokkel in ${utledInnsatsgrupper(innsatsgruppe).toSanityListe()}
    """.trimIndent()
}

private fun List<String>.toSanityListe(): String {
    return "[${this.joinToString { "'$it'" }}]"
}

fun utledInnsatsgrupper(innsatsgruppe: String): List<String> {
    return when (innsatsgruppe) {
        Innsatsgruppe.STANDARD_INNSATS.name -> listOf(Innsatsgruppe.STANDARD_INNSATS.name)
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
        )

        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name,
        )

        Innsatsgruppe.VARIG_TILPASSET_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name,
            Innsatsgruppe.VARIG_TILPASSET_INNSATS.name,
        )

        else -> emptyList()
    }
}
