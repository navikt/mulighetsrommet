package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.Bestillingstype

/**
 * Informasjon om kontering av tiltak i OeBS.
 * Regnskapskonto og artskonto gjelder per [tiltakskode] for gitt [periode].
 */
data class OebsKontering(
    val bestillingstype: Bestillingstype,
    val tiltakskode: Tiltakskode,
    val periode: Periode,
    val statligRegnskapskonto: String,
    val statligArtskonto: String,
)
