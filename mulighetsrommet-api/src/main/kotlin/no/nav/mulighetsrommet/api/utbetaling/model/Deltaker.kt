package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Deltaker(
    val id: UUID,
    val gjennomforingId: UUID,
    val norskIdent: NorskIdent?,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val registrertDato: LocalDate,
    val endretTidspunkt: LocalDateTime,
    val deltakelsesprosent: Double?,
    val status: DeltakerStatus,
)

data class Deltakelsesmengde(
    val gyldigFra: LocalDate,
    val deltakelsesprosent: Double,
)

data class Person(
    val norskIdent: NorskIdent,
    val foedselsdato: LocalDate?,
    val navn: String,
    val geografiskEnhet: NavEnhetDto?,
    val region: NavEnhetDto?,
)
