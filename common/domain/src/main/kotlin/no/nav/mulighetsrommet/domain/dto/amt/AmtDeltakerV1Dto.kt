package no.nav.mulighetsrommet.domain.dto.amt

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AmtDeltakerV1Dto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val personIdent: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: AmtDeltakerStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endretDato: LocalDateTime,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
)

@Serializable
data class AmtDeltakerStatus(
    val type: Type,
    val aarsak: Aarsak?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetDato: LocalDateTime,
) {
    enum class Type {
        VENTER_PA_OPPSTART,
        DELTAR,
        HAR_SLUTTET,
        IKKE_AKTUELL,
        FEILREGISTRERT,
        PABEGYNT_REGISTRERING,
        SOKT_INN,
        VURDERES,
        VENTELISTE,
        AVBRUTT,
        FULLFORT,
        UTKAST_TIL_PAMELDING,
        AVBRUTT_UTKAST,
    }

    enum class Aarsak {
        SYK,
        FATT_JOBB,
        TRENGER_ANNEN_STOTTE,
        FIKK_IKKE_PLASS,
        UTDANNING,
        FERDIG,
        AVLYST_KONTRAKT,
        IKKE_MOTT,
        FEILREGISTRERT,
        OPPFYLLER_IKKE_KRAVENE,
        ANNET,
    }
}
