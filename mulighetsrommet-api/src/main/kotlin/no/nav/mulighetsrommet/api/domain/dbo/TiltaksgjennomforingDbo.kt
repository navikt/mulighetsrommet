package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.util.*

data class TiltaksgjennomforingDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val arrangorVirksomhetId: UUID,
    val arrangorKontaktpersoner: List<UUID>,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val apentForInnsok: Boolean,
    val antallPlasser: Int,
    val avtaleId: UUID,
    val administratorer: List<NavIdent>,
    val navRegion: String,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktpersonDbo>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
    val estimertVentetidVerdi: Int?,
    val estimertVentetidEnhet: String?,
)

data class TiltaksgjennomforingKontaktpersonDbo(
    val navIdent: NavIdent,
    val navEnheter: List<String>,
    val beskrivelse: String?,
)
