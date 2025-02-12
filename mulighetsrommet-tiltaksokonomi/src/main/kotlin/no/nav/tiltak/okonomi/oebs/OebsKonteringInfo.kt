package no.nav.tiltak.okonomi.oebs

import no.nav.mulighetsrommet.model.Tiltakskode

/**
 * Betalinger må være knyttet til en statsregnskapskonto.
 *
 * En statsregnskapskonto består av 12 tegn og representerer kapittelpost og underpost.
 *
 * Verdien er den samme for alle tiltakstyper.
 */
enum class OebsKontering(val statsregnskapskonto: String) {
    TILTAK(statsregnskapskonto = "063476300000"),
}

/**
 * Hver tiltakstype er representert med en egen artskonto i OeBS.
 */
enum class OebsKonteringInfo(val artskonto: String) {
    ARBEIDSFORBEREDENDE_TRENING(
        artskonto = "TODO",
    ),
    ARBEIDSRETTET_REHABILITERING(
        artskonto = "TODO",
    ),
    AVKLARING(
        artskonto = "TODO",
    ),
    DIGITALT_OPPFOLGINGSTILTAK(
        artskonto = "TODO",
    ),
    GRUPPE_ARBEIDSMARKEDSOPPLAERING(
        artskonto = "TODO",
    ),
    GRUPPE_FAG_OG_YRKESOPPLAERING(
        artskonto = "TODO",
    ),
    JOBBKLUBB(
        artskonto = "TODO",
    ),
    OPPFOLGING(
        artskonto = "TODO",
    ),
    VARIG_TILRETTELAGT_ARBEID_SKJERMET(
        artskonto = "TODO",
    ),
    ;

    companion object {
        fun getArtskonto(tiltakskode: Tiltakskode): String {
            val kontering = when (tiltakskode) {
                Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> ARBEIDSFORBEREDENDE_TRENING
                Tiltakskode.ARBEIDSRETTET_REHABILITERING -> ARBEIDSRETTET_REHABILITERING
                Tiltakskode.AVKLARING -> AVKLARING
                Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> DIGITALT_OPPFOLGINGSTILTAK
                Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> GRUPPE_ARBEIDSMARKEDSOPPLAERING
                Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> GRUPPE_FAG_OG_YRKESOPPLAERING
                Tiltakskode.JOBBKLUBB -> JOBBKLUBB
                Tiltakskode.OPPFOLGING -> OPPFOLGING
                Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> VARIG_TILRETTELAGT_ARBEID_SKJERMET
            }

            return kontering.artskonto
        }
    }
}
