@file:UseSerializers(LocalDateSerializer::class, UUIDSerializer::class)

package no.nav.mulighetsrommet.admin.gjennomforing

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ArenaEnkeltplassDto(
    val id: UUID,
    val tiltaksnummer: Tiltaksnummer,
    val navn: String,
    val tiltakstype: Gjennomforing.Tiltakstype,
    val arrangor: Gjennomforing.ArrangorUnderenhet,
    val ansvarligEnhet: NavEnhetNummer,
    val deltakerStatus: DeltakerStatusType,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val deltaker: DeltakerDto,
)
