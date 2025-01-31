package no.nav.mulighetsrommet.tiltak.okonomi.oebs

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
}
