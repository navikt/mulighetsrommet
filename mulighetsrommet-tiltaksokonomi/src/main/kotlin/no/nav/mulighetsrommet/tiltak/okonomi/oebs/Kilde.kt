package no.nav.mulighetsrommet.tiltak.okonomi.oebs

/**
 * Representerer hvilket system som har sendt bestillinger eller fakturaer til OeBS.
 *
 * Lengden på kildeverdien er begrenset til 8 tegn (tror vi).
 */
enum class Kilde {
    /**
     * Tiltaksadministrasjon
     */
    TILTADM,
}
