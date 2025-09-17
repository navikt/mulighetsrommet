package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.model.Tiltakskode

enum class Prismodell(val beskrivelse: String) {
    ANNEN_AVTALT_PRIS("Annen avtalt pris"),
    FORHANDSGODKJENT_PRIS_PER_MANEDSVERK("Fast sats per tiltaksplass per måned"),
    AVTALT_PRIS_PER_MANEDSVERK("Avtalt månedspris per tiltaksplass"),
    AVTALT_PRIS_PER_UKESVERK("Avtalt ukespris per tiltaksplass"),
    AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER("Avtalt pris per time oppfølging per deltaker"),
}

object Prismodeller {
    fun getPrismodellerForTiltak(tiltakskode: Tiltakskode): List<Prismodell> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        -> listOf(
            Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        )

        Tiltakskode.AVKLARING,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        -> listOf(
            Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
            Prismodell.AVTALT_PRIS_PER_UKESVERK,
            Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            Prismodell.ANNEN_AVTALT_PRIS,
        )

        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        -> listOf(
            Prismodell.ANNEN_AVTALT_PRIS,
        )
    }
}
