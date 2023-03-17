package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate? = null,
    val status: Tiltaksgjennomforingsstatus,
    val virksomhetsnummer: String
) {

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String
    )

    companion object {
        fun from(tiltaksgjennomforing: TiltaksgjennomforingAdminDto) = tiltaksgjennomforing.run {
            TiltaksgjennomforingDto(
                id = id,
                tiltakstype = tiltakstype.run {
                    Tiltakstype(
                        id = id,
                        navn = navn,
                        arenaKode = arenaKode
                    )
                },
                navn = navn,
                startDato = startDato,
                sluttDato = sluttDato,
                status = status,
                virksomhetsnummer = virksomhetsnummer
            )
        }
    }
}
