package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ArenaDeltakerDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val norskIdent: NorskIdent,
    val arenaTiltakskode: String,
    val status: ArenaDeltakerStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startDato: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sluttDato: LocalDateTime?,
    val beskrivelse: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertIArenaDato: LocalDateTime,
)

enum class ArenaDeltakerStatus {
    AVSLAG,
    IKKE_AKTUELL,
    TAKKET_NEI_TIL_TILBUD,
    TILBUD,
    TAKKET_JA_TIL_TILBUD,
    INFORMASJONSMOTE,
    AKTUELL,
    VENTELISTE,
    GJENNOMFORES,
    DELTAKELSE_AVBRUTT,
    GJENNOMFORING_AVBRUTT,
    GJENNOMFORING_AVLYST,
    FULLFORT,
    IKKE_MOTT,
}
