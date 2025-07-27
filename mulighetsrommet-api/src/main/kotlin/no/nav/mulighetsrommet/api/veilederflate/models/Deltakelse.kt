package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.ArbeidsgiverAvtaleStatus
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class Deltakelse {
    abstract val id: UUID
    abstract val eierskap: Eierskap
    abstract val tittel: String
    abstract val tiltakstypeNavn: String
    abstract val innsoktDato: LocalDate?
    abstract val sistEndretDato: LocalDate?
    abstract val periode: Periode

    @Serializable
    enum class Eierskap {
        ARENA,
        TEAM_KOMET,
        TEAM_TILTAK,
    }

    @Serializable
    data class Periode(
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
    )

    @Serializable
    data class DeltakelseArena(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val eierskap: Eierskap,
        override val tittel: String,
        override val tiltakstypeNavn: String,
        @Serializable(with = LocalDateSerializer::class)
        override val innsoktDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sistEndretDato: LocalDate?,
        override val periode: Periode,
        val status: DeltakelseArenaStatus,
    ) : Deltakelse() {
        @Serializable
        data class DeltakelseArenaStatus(
            val type: ArenaDeltakerStatus,
            val visningstekst: String,
        )
    }

    @Serializable
    data class DeltakelseGruppetiltak(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val eierskap: Eierskap,
        override val tittel: String,
        override val tiltakstypeNavn: String,
        @Serializable(with = LocalDateSerializer::class)
        override val innsoktDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sistEndretDato: LocalDate?,
        override val periode: Periode,
        val status: DeltakelseGruppetiltakStatus,
        @Serializable(with = UUIDSerializer::class)
        val gjennomforingId: UUID,
    ) : Deltakelse() {
        @Serializable
        data class DeltakelseGruppetiltakStatus(
            val type: DeltakerStatusType,
            val visningstekst: String,
            val aarsak: String?,
        )
    }

    @Serializable
    data class DeltakelseArbeidsgiverAvtale(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val eierskap: Eierskap,
        override val tittel: String,
        override val tiltakstypeNavn: String,
        @Serializable(with = LocalDateSerializer::class)
        override val innsoktDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sistEndretDato: LocalDate?,
        override val periode: Periode,
        val status: DeltakelseArbeidsgiverAvtaleStatus,
    ) : Deltakelse() {
        @Serializable
        data class DeltakelseArbeidsgiverAvtaleStatus(
            val type: ArbeidsgiverAvtaleStatus,
            val visningstekst: String,
        )
    }
}
