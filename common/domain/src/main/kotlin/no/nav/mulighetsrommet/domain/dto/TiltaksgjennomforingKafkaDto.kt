package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingKafkaDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: TiltakstypeDto,
    val navn: String?,
    @Serializable(with = LocalDateSerializer::class)
    val fraDato: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val tilDato: LocalDate? = null,
) {
    companion object {
        fun from(tiltaksgjennomforing: TiltaksgjennomforingDto) = tiltaksgjennomforing.run {
            TiltaksgjennomforingKafkaDto(
                id = id,
                tiltakstype = tiltakstype,
                navn = navn,
                fraDato = fraDato?.let { LocalDate.from(it) },
                tilDato = tilDato?.let { LocalDate.from(it) },
            )
        }
    }
}
