package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.model.Tiltakskode

enum class PrismodellType(val beskrivelse: String) {
    ANNEN_AVTALT_PRIS("Annen avtalt pris"),
    FORHANDSGODKJENT_PRIS_PER_MANEDSVERK("Fast sats per tiltaksplass per måned"),
    AVTALT_PRIS_PER_MANEDSVERK("Avtalt månedspris per tiltaksplass"),
    AVTALT_PRIS_PER_UKESVERK("Avtalt ukespris per tiltaksplass"),
    AVTALT_PRIS_PER_HELE_UKESVERK("Avtalt pris per uke med påbegynt oppfølging per deltaker"),
    AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER("Avtalt pris per time oppfølging per deltaker"),
}

object Prismodeller {
    fun getPrismodellerForTiltak(tiltakskode: Tiltakskode): List<PrismodellType> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        -> listOf(
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        )

        Tiltakskode.AVKLARING,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        -> listOf(
            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            PrismodellType.ANNEN_AVTALT_PRIS,
        )

        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        -> listOf(
            PrismodellType.ANNEN_AVTALT_PRIS,
        )
    }
}
