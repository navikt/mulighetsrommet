package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class Deltakelse {
    abstract val id: UUID
    abstract val tilstand: DeltakelseTilstand
    abstract val tiltakstype: DeltakelseTiltakstype
    abstract val periode: DeltakelsePeriode
    abstract val tittel: String
    abstract val status: DeltakelseStatus

    @Serializable
    @SerialName("TILTAKSADMINISTRASJON")
    data class TiltaksadministrasjonDeltakelse(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val tilstand: DeltakelseTilstand,
        override val tiltakstype: DeltakelseTiltakstype,
        override val periode: DeltakelsePeriode,
        override val tittel: String,
        override val status: DeltakelseStatus,
        val tiltakskode: Tiltakskode,
        @Serializable(with = LocalDateSerializer::class)
        val innsoktDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val sistEndretDato: LocalDate?,
        @Serializable(with = UUIDSerializer::class)
        val gjennomforingId: UUID,
        val infoMeldingStatus: InfoMeldingStatus?,
    ) : Deltakelse() {
        enum class InfoMeldingStatus {
            VENTER_PA_OPPSTART,
            DELTAR,
            UTKAST_TIL_PAMELDING,
            KLADD,
            SOKT_INN,
            VENTELISTE,
            VURDERES,
        }
    }

    @Serializable
    @SerialName("ARENA")
    data class ArenaDeltakelse(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val tilstand: DeltakelseTilstand,
        override val tiltakstype: DeltakelseTiltakstype,
        override val periode: DeltakelsePeriode,
        override val tittel: String,
        override val status: DeltakelseStatus,
    ) : Deltakelse()

    @Serializable
    @SerialName("TILTAK_ARBEIDSGIVER")
    data class TiltakArbeidsgiverDeltakelse(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val tilstand: DeltakelseTilstand,
        override val tiltakstype: DeltakelseTiltakstype,
        override val periode: DeltakelsePeriode,
        override val tittel: String,
        override val status: DeltakelseStatus,
    ) : Deltakelse()
}

@Serializable
data class DeltakelseTiltakstype(
    val navn: String,
)

@Serializable
enum class DeltakelseTilstand {
    UTKAST,
    KLADD,
    AKTIV,
    AVSLUTTET,
}

@Serializable
data class DeltakelseStatus(
    val type: DataElement.Status,
    val aarsak: String?,
)

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)
