package no.nav.mulighetsrommet.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
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
