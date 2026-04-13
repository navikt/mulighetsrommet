package no.nav.mulighetsrommet.api.tiltakstype.model

import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Regelverklenke
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import java.time.LocalDate
import java.util.UUID

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val tiltakskode: Tiltakskode?,
    val arenakode: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: TiltakstypeStatus,
    val sanityId: UUID?,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val regelverklenker: List<Regelverklenke>,
    val kanKombineresMed: List<String>,
)
