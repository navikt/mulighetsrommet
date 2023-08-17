package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class ArenaMigreringTiltaksgjennomforingDbo(
    val id: UUID,
    val tiltakskode: String,
    val navn: String,
    val arrangorOrganisasjonsnummer: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val avslutningsstatus: Avslutningsstatus,
    val tilgjengelighet: TiltaksgjennomforingTilgjengelighetsstatus,
    val antallPlasser: Int,
    val createdAt: LocalDateTime,
)
