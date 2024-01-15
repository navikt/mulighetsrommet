package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class ArenaTiltaksgjennomforingDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
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
    val apentForInnsok: Boolean,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    val deltidsprosent: Double,
)
