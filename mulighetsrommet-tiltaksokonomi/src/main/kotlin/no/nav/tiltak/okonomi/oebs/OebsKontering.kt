package no.nav.tiltak.okonomi.oebs

import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Artskonto
import no.nav.tiltak.okonomi.Regnskapskonto

/**
 * Informasjon om kontering av tiltak i OeBS.
 * Kontonumrene gjelder per [regnskapskonto] og [artskonto] for en gitt [periode].
 */
data class OebsKontering(
    val regnskapskonto: Regnskapskonto,
    val artskonto: Artskonto,
    val periode: Periode,
    val statligRegnskapskonto: String,
    val statligArtskonto: String,
)
