package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Avslutningsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String?,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val tiltaksnummer: String,
    val virksomhetsnummer: String?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate? = null,
    val enhet: String,
    val avslutningsstatus: Avslutningsstatus
)
