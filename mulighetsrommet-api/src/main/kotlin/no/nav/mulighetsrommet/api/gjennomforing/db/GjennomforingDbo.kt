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

enum class GjennomforingType {
    ARENA,
    AVTALE,
    ENKELTPLASS,
}

sealed class GjennomforingDbo {
    abstract val id: UUID
}

data class GjennomforingAvtaleDbo(
    override val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val arrangorKontaktpersoner: List<UUID>,
    val avtaleId: UUID,
    val prismodellId: UUID,
    val administratorer: List<NavIdent>,
    val navEnheter: Set<NavEnhetNummer>,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val kontaktpersoner: List<GjennomforingKontaktpersonDbo>,
    val oppmoteSted: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val estimertVentetidVerdi: Int?,
    val estimertVentetidEnhet: String?,
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
) : GjennomforingDbo()

data class GjennomforingEnkeltplassDbo(
    override val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
) : GjennomforingDbo()

data class GjennomforingArenaDbo(
    override val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val tiltaksnummer: Tiltaksnummer,
    val arenaAnsvarligEnhet: String?,
) : GjennomforingDbo()

data class GjennomforingBaseDbo(
    val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val type: GjennomforingType,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
)

data class GjennomforingArenaDataDbo(
    val id: UUID,
    val tiltaksnummer: Tiltaksnummer? = null,
    val arenaAnsvarligEnhet: String? = null,
)

data class GjennomforingKontaktpersonDbo(
    val navIdent: NavIdent,
    val beskrivelse: String?,
)
