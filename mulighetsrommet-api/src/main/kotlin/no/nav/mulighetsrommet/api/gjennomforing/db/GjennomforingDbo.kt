package no.nav.mulighetsrommet.api.gjennomforing.db

import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.*

data class GjennomforingDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val arrangorKontaktpersoner: List<UUID>,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val antallPlasser: Int,
    val avtaleId: UUID,
    val administratorer: List<NavIdent>,
    val navEnheter: List<String>,
    val oppstart: GjennomforingOppstartstype,
    val kontaktpersoner: List<GjennomforingKontaktpersonDbo>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
    val estimertVentetidVerdi: Int?,
    val estimertVentetidEnhet: String?,
    val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
)

data class GjennomforingKontaktpersonDbo(
    val navIdent: NavIdent,
    val navEnheter: List<String>,
    val beskrivelse: String?,
)
