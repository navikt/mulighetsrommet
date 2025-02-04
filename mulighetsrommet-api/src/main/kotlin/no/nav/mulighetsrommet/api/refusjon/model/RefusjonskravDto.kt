package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class RefusjonskravDto(
    val id: UUID,
    val status: RefusjonskravStatus,
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: Tiltakstype,
    val gjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val beregning: RefusjonKravBeregning,
    val betalingsinformasjon: Betalingsinformasjon,
    val journalpostId: String?,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
) {
    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class Tiltakstype(
        val navn: String,
    )

    @Serializable
    data class Betalingsinformasjon(
        val kontonummer: Kontonummer?,
        val kid: Kid?,
    )
}
