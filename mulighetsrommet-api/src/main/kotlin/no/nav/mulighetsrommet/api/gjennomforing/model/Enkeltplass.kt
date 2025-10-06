package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Enkeltplass(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val oppdatertTidspunkt: LocalDateTime,
    val tiltakstype: Tiltakstype,
    val arrangor: Arrangor,
    val arena: ArenaData?,
) {

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
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
    data class ArenaData(
        val tiltaksnummer: String?,
        val navn: String?,
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
        val status: GjennomforingStatusType?,
        val arenaAnsvarligEnhet: String?,
    )
}
