package no.nav.mulighetsrommet.model

enum class Avtaletype(val beskrivelse: String) {
    AVTALE("Avtale"),
    RAMMEAVTALE("Rammeavtale"),
    FORHANDSGODKJENT("Forhåndsgodkjent"),
    OFFENTLIG_OFFENTLIG("Offentlig-offentlig samarbeid"), ;

    fun kreverSakarkivNummer(): Boolean {
        return listOf(RAMMEAVTALE, AVTALE).contains(this)
    }
}

object Avtaletyper {
    fun getAvtaletyperForTiltak(tiltakskode: Tiltakskode): List<Avtaletype> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        -> listOf(
            Avtaletype.FORHANDSGODKJENT,
        )

        Tiltakskode.AVKLARING,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        -> listOf(
            Avtaletype.RAMMEAVTALE,
            Avtaletype.AVTALE,
        )

        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        -> listOf(
            Avtaletype.RAMMEAVTALE,
            Avtaletype.AVTALE,
            Avtaletype.OFFENTLIG_OFFENTLIG,
        )
    }
}
