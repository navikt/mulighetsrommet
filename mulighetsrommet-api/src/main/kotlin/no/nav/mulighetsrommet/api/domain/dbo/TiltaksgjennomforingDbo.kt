package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import java.time.LocalDate
import java.util.*

data class TiltaksgjennomforingDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val tiltaksnummer: String?,
    val arrangorOrganisasjonsnummer: String,
    val arrangorKontaktpersonId: UUID?,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: String?,
    val avslutningsstatus: Avslutningsstatus,
    val tilgjengelighet: TiltaksgjennomforingTilgjengelighetsstatus,
    val estimertVentetid: String?,
    val antallPlasser: Int?,
    val avtaleId: UUID? = null,
    val ansvarlige: List<String>,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    val stengtFra: LocalDate?,
    val stengtTil: LocalDate?,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktpersonDbo>,
    val lokasjonArrangor: String?,
)

data class TiltaksgjennomforingKontaktpersonDbo(
    val navIdent: String,
    val navEnheter: List<String>,
)
