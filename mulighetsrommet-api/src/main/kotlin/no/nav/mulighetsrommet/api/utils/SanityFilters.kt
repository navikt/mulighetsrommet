package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe

fun byggLokasjonsFilter(lokasjoner: List<String>): String {
    if (lokasjoner.isEmpty()) return ""

    return """
            && lokasjon in ${lokasjoner.toSanityListe()}
    """.trimIndent()
}

fun byggEnhetOgFylkeFilter(enhetsId: String, fylkeId: String?): String {
    return """
            && ('enhet.lokal.$enhetsId' in enheter[]._ref || (enheter[0] == null && 'enhet.fylke.$fylkeId' == fylke._ref))
    """.trimIndent()
}

fun byggTiltakstypeFilter(tiltakstyper: List<String>): String {
    if (tiltakstyper.isEmpty()) return ""

    return """
            && tiltakstype->_id in ${tiltakstyper.toSanityListe()}]
    """.trimIndent()
}

fun byggSokefilter(sokestreng: String): String {
    if (sokestreng.isBlank()) return ""

    return """
            && [tiltaksgjennomforingNavn, string(tiltaksnummer.current), tiltakstype->tiltakstypeNavn, lokasjon, kontaktinfoArrangor->selskapsnavn, oppstartsdato] match "*$sokestreng*"
    """.trimIndent()
}

fun byggInnsatsgruppeFilter(innsatsgruppe: String?): String {
    if (innsatsgruppe == null) return ""

    return """
             && tiltakstype->innsatsgruppe->nokkel in ${utledInnsatsgrupper(innsatsgruppe).toSanityListe()}
    """.trimIndent()
}

private fun List<String>.toSanityListe(): String {
    return "[${this.joinToString { "'$it'" }}]"
}

private fun utledInnsatsgrupper(innsatsgruppe: String): List<String> {
    return when (innsatsgruppe) {
        Innsatsgruppe.STANDARD_INNSATS.name -> listOf(Innsatsgruppe.STANDARD_INNSATS.name)
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name
        )

        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name
        )

        Innsatsgruppe.VARIG_TILPASSET_INNSATS.name -> listOf(
            Innsatsgruppe.VARIG_TILPASSET_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name,
            Innsatsgruppe.VARIG_TILPASSET_INNSATS.name
        )

        else -> emptyList()
    }
}
