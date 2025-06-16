package no.nav.mulighetsrommet.model

import no.nav.mulighetsrommet.model.Tiltakskode.*

enum class Avtaletype(val beskrivelse: String) {
    AVTALE("Avtale"),
    RAMMEAVTALE("Rammeavtale"),
    FORHANDSGODKJENT("Forhåndsgodkjent"),
    OFFENTLIG_OFFENTLIG("Offentlig-offentlig samarbeid"), ;

    fun kreverSakarkivNummer(): Boolean {
        return listOf(RAMMEAVTALE, AVTALE).contains(this)
    }
}

fun allowedAvtaletypes(tiltakskode: Tiltakskode): List<Avtaletype> = when (tiltakskode) {
    ARBEIDSFORBEREDENDE_TRENING, VARIG_TILRETTELAGT_ARBEID_SKJERMET ->
        listOf(Avtaletype.FORHANDSGODKJENT)

    AVKLARING, OPPFOLGING, ARBEIDSRETTET_REHABILITERING, JOBBKLUBB, DIGITALT_OPPFOLGINGSTILTAK ->
        listOf(Avtaletype.RAMMEAVTALE, Avtaletype.AVTALE)

    GRUPPE_ARBEIDSMARKEDSOPPLAERING, GRUPPE_FAG_OG_YRKESOPPLAERING ->
        listOf(Avtaletype.RAMMEAVTALE, Avtaletype.AVTALE, Avtaletype.OFFENTLIG_OFFENTLIG)
}
