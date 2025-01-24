package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.GjennomforingOppstartstype
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingEksternV1Dto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: GjennomforingStatus,
    val virksomhetsnummer: String,
    val oppstart: GjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
    val apentForPamelding: Boolean,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
        val tiltakskode: Tiltakskode,
    )
}
