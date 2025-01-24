package no.nav.mulighetsrommet.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class ArenaGjennomforingDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val tiltaksnummer: String,
    val arrangorOrganisasjonsnummer: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: String?,
    val avslutningsstatus: Avslutningsstatus,
    val apentForPamelding: Boolean,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val deltidsprosent: Double,
)
