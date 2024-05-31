package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
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
    val sluttDato: LocalDate?,
    val status: TiltaksgjennomforingStatus.Enum,
    val virksomhetsnummer: String,
    val oppstart: TiltaksgjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
    val nusData: NusDataTilDvh?,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
    )

    companion object {
        fun from(tiltaksgjennomforing: TiltaksgjennomforingAdminDto) = tiltaksgjennomforing.run {
            TiltaksgjennomforingDto(
                id = id,
                tiltakstype = Tiltakstype(
                    id = tiltakstype.id,
                    navn = tiltakstype.navn,
                    arenaKode = tiltakstype.arenaKode,
                ),
                navn = navn,
                startDato = startDato,
                sluttDato = sluttDato,
                status = status.enum,
                virksomhetsnummer = arrangor.organisasjonsnummer,
                oppstart = oppstart,
                tilgjengeligForArrangorFraOgMedDato = tilgjengeligForArrangorFraOgMedDato,
                nusData = nusData?.let {
                    NusDataTilDvh(
                        versjon = it.versjon,
                        kategorier = it.utdanningskategorier.map { kat -> kat.code },
                    )
                },
            )
        }
    }
}

@Serializable
data class NusDataTilDvh(
    val versjon: String,
    val kategorier: List<String>,
)
