package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val tiltaksnummer: String?,
    val arrangorOrganisasjonsnummer: String,
    @Serializable(with = UUIDSerializer::class)
    val arrangorKontaktpersonId: UUID?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: String?,
    val avslutningsstatus: Avslutningsstatus,
    val tilgjengelighet: TiltaksgjennomforingTilgjengelighetsstatus,
    val estimertVentetid: String?,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
    val ansvarlige: List<String>,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate?,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktpersonDbo>,
    val lokasjonArrangor: String?,
)

@Serializable
data class TiltaksgjennomforingKontaktpersonDbo(
    val navIdent: String,
    val navEnheter: List<String>,
)
