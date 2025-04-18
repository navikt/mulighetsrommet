package no.nav.mulighetsrommet.model

import no.nav.mulighetsrommet.model.Tiltakskode.*

enum class Avtaletype {
    Avtale,
    Rammeavtale,
    Forhaandsgodkjent,
    OffentligOffentlig, ;

    fun kreverSakarkivNummer(): Boolean {
        return listOf(Rammeavtale, Avtale).contains(this)
    }
}

fun allowedAvtaletypes(tiltakskode: Tiltakskode?): List<Avtaletype> = when (tiltakskode) {
    ARBEIDSFORBEREDENDE_TRENING, VARIG_TILRETTELAGT_ARBEID_SKJERMET ->
        listOf(Avtaletype.Forhaandsgodkjent)

    AVKLARING, OPPFOLGING, ARBEIDSRETTET_REHABILITERING, JOBBKLUBB, DIGITALT_OPPFOLGINGSTILTAK ->
        listOf(Avtaletype.Rammeavtale, Avtaletype.Avtale)

    GRUPPE_ARBEIDSMARKEDSOPPLAERING, GRUPPE_FAG_OG_YRKESOPPLAERING ->
        listOf(Avtaletype.Rammeavtale, Avtaletype.Avtale, Avtaletype.OffentligOffentlig)

    else -> Avtaletype.entries
}
