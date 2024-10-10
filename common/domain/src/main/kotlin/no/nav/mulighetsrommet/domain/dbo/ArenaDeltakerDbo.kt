package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.ArenaDeltakerStatus
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
