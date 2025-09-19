package no.nav.mulighetsrommet.api.gjennomforing.db

import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.model.*
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
    val status: GjennomforingStatusType,
    val antallPlasser: Int,
    val avtaleId: UUID,
    val administratorer: List<NavIdent>,
    val navEnheter: Set<NavEnhetNummer>,
    val oppstart: GjennomforingOppstartstype,
    val kontaktpersoner: List<GjennomforingKontaktpersonDbo>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
    val estimertVentetidVerdi: Int?,
    val estimertVentetidEnhet: Gjennomforing.EstimertVentetid.Enhet?,
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
)

data class GjennomforingKontaktpersonDbo(
    val navIdent: NavIdent,
    val beskrivelse: String?,
)
