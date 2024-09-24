package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class DeltakerKort(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID? = null,
    val eierskap: Eierskap,
    val tittel: String,
    val tiltakstypeNavn: String,
    val status: DeltakerStatus,
    @Serializable(with = LocalDateSerializer::class)
    val innsoktDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sistEndretDato: LocalDate?,
    val periode: Periode,
) {
    @Serializable
    data class Periode(
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
    )

    @Serializable
    data class DeltakerStatus(
        val type: DeltakerStatusType,
        val visningstekst: String,
        val aarsak: String?,

    ) {
        @Serializable
        enum class DeltakerStatusType {
            KLADD,
            UTKAST_TIL_PAMELDING,
            AVBRUTT_UTKAST,
            VENTER_PA_OPPSTART,
            DELTAR,
            HAR_SLUTTET,
            IKKE_AKTUELL,
            SOKT_INN,
            VURDERES,
            VENTELISTE,
            AVBRUTT,
            FULLFORT,
            FEILREGISTRERT,
            AVSLAG,
            TAKKET_NEI_TIL_TILBUD,
            TILBUD,
            TAKKET_JA_TIL_TILBUD,
            INFORMASJONSMOTE,
            AKTUELL,
            GJENNOMFORES,
            DELTAKELSE_AVBRUTT,
            GJENNOMFORING_AVBRUTT,
            GJENNOMFORING_AVLYST,
            IKKE_MOTT,
            PABEGYNT_REGISTRERING,

            // TODO egne enums per opphav
            ANNULLERT,
            PAABEGYNT,
            MANGLER_GODKJENNING,
            KLAR_FOR_OPPSTART,
            AVSLUTTET,
        }
    }

    @Serializable
    enum class Eierskap {
        ARENA,
        KOMET,
        TEAM_TILTAK,
    }
}
