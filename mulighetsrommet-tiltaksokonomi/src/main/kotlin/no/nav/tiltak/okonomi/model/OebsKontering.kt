package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.Tilskuddstype

/**
 * Informasjon om kontering av tiltak i OeBS.
 * Regnskapskonto og artskonto gjelder per [tilskuddstype] og [tiltakskode] for en gitt [periode].
 */
data class OebsKontering(
    val tilskuddstype: Tilskuddstype,
    val tiltakskode: Tiltakskode,
    val periode: Periode,
    val statligRegnskapskonto: String,
    val statligArtskonto: String,
)
