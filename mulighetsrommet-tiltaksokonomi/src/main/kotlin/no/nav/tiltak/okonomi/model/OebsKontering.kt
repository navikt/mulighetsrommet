package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode

/**
 * Informasjon om kontering av tiltak i OeBS.
 * Regnskapskonto og artskonto gjelder per [tiltakskode] for gitt [periode].
 */
data class OebsKontering(
    val tiltakskode: Tiltakskode,
    val periode: Periode,
    val statligRegnskapskonto: String,
    val statligArtskonto: String,
)
