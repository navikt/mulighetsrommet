package no.nav.mulighetsrommet.api.gjennomforing.db

import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.UUID

data class GjennomforingDbo(
    val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
)

data class GjennomforingArenaDataDbo(
    val id: UUID,
    val tiltaksnummer: Tiltaksnummer? = null,
    val navn: String? = null,
    val startDato: LocalDate? = null,
    val sluttDato: LocalDate? = null,
    val status: GjennomforingStatusType? = null,
    val arenaAnsvarligEnhet: String? = null,
)

data class GjennomforingGruppeDbo(
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
    val oppmoteSted: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
    val estimertVentetidVerdi: Int?,
    val estimertVentetidEnhet: String?,
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
    val pameldingType: GjennomforingPameldingType,
)

data class GjennomforingKontaktpersonDbo(
    val navIdent: NavIdent,
    val beskrivelse: String?,
)
