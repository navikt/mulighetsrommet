package no.nav.mulighetsrommet.kafka.amt

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
    val status: Status,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertDato: LocalDateTime,
    val dagerPerUke: Int?,
    val prosentStilling: Float?,
) {
    enum class Status {
        VENTER_PA_OPPSTART,
        DELTAR,
        HAR_SLUTTET,
        IKKE_AKTUELL,
        FEILREGISTRERT,
        PABEGYNT_REGISTRERING,

        /** PABEGYNT er erstattet av PABEGYNT_REGISTRERING, men status kan fortsatt være på topic */
        PABEGYNT,

        AVBRUTT,
        SOKT_INN,
        VENTELISTE,
    }
}
