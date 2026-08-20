package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class ArrangorflateTiltakRadDto(
    val gjennomforing: ArrangorflateGjennomforingDto,
    val arrangor: ArrangorflateArrangorDto,
    val tiltakstype: ArrangorflateTiltakstypeDto,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)

fun ArrangorflateTiltak.toRadDto(): ArrangorflateTiltakRadDto = ArrangorflateTiltakRadDto(
    gjennomforing = ArrangorflateGjennomforingDto(
        id = id,
        navn = navn,
        lopenummer = lopenummer,
    ),
    arrangor = ArrangorflateArrangorDto(
        id = arrangor.id,
        organisasjonsnummer = arrangor.organisasjonsnummer,
        navn = arrangor.navn,
    ),
    tiltakstype = ArrangorflateTiltakstypeDto(
        navn = tiltakstype.navn,
        tiltakskode = tiltakstype.tiltakskode,
    ),
    startDato = startDato,
    sluttDato = sluttDato,
)
