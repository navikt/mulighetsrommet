package no.nav.mulighetsrommet.api.okonomi.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class ArrangorflateTilsagn(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val gjennomforing: Gjennomforing,
    val tiltakstype: Tiltakstype,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val beregning: Prismodell.TilsagnBeregning,
    val arrangor: Arrangor,
) {
    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
    )

    @Serializable
    data class Gjennomforing(
        val navn: String,
    )

    @Serializable
    data class Tiltakstype(
        val navn: String,
    )
}
