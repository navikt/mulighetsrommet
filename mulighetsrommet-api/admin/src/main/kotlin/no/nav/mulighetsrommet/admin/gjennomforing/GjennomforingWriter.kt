package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.Either
import no.nav.mulighetsrommet.api.domain.gjennomforing.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

/**
 * Skriveport for gjennomføringer. Implementeres i persistence.
 */
interface GjennomforingWriter {
    fun upsert(gjennomforing: UpsertGjennomforing)

    fun updateDetaljer(detaljer: UpdateGjennomforingDetaljer)

    fun setEstimertVentetid(id: UUID, verdi: Int?, enhet: String?)

    fun setRedaksjoneltInnhold(id: UUID, beskrivelse: String?, faneinnhold: Faneinnhold?)

    fun setNavEnheter(id: UUID, navEnheter: Set<NavEnhetNummer>)

    fun setAdministratorer(id: UUID, administratorer: Set<NavIdent>)

    fun setKontaktpersoner(id: UUID, kontaktpersoner: Set<SetGjennomforingKontaktperson>)

    fun setArrangorKontaktpersoner(id: UUID, arrangorKontaktpersoner: Set<UUID>)

    fun setArenaData(arenaData: SetGjennomforingArenaData)

    fun setPrismodellId(gjennomforingId: UUID, prismodellId: UUID)

    fun setFreeTextSearch(id: UUID, content: List<String>)

    fun setPublisert(id: UUID, publisert: Boolean): Int

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean): Int

    fun setTilgjengeligForArrangorDato(id: UUID, date: LocalDate): Int

    fun setStatus(
        id: UUID,
        status: GjennomforingStatusType,
        sluttDato: LocalDate?,
        aarsaker: List<AvbrytGjennomforingAarsak>?,
        forklaring: String?,
    ): Int

    fun frikobleKontaktpersonFraGjennomforing(kontaktpersonId: UUID, gjennomforingId: UUID)

    fun setStengtHosArrangor(id: UUID, periode: Periode, beskrivelse: String): Either<StengtPeriodeOverlapper, Unit>

    fun deleteStengtHosArrangor(id: Int)

    fun aquireLock(id: UUID)
}

/**
 * Perioden overlapper med en annen registrert stengt-periode for gjennomføringen.
 */
data object StengtPeriodeOverlapper

data class UpsertGjennomforing(
    val id: UUID,
    val type: GjennomforingType,
    val tiltakskode: Tiltakskode,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val avtaleId: UUID?,
    val prismodellId: UUID?,
    val ansvarligEnhet: NavEnhetNummer?,
    val arenaTiltaksnummer: Tiltaksnummer?,
    val arenaAnsvarligEnhet: String?,
)

data class UpdateGjennomforingDetaljer(
    val id: UUID,
    val arrangorId: UUID,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val navn: String,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val prismodellId: UUID?,
    val oppmoteSted: String?,
    val tilgjengeligForArrangorDato: LocalDate?,
)

data class SetGjennomforingArenaData(
    val id: UUID,
    val tiltaksnummer: Tiltaksnummer? = null,
    val arenaAnsvarligEnhet: String? = null,
)

data class SetGjennomforingKontaktperson(
    val navIdent: NavIdent,
    val beskrivelse: String?,
)

/**
 * Ventende prisendring for en enkeltplass. Implementeres i persistence.
 */
interface EnkeltplassPrisendringHandler {
    fun insert(prisendring: EnkeltplassPrisendringRef)

    fun getByGjennomforingId(gjennomforingId: UUID): EnkeltplassPrisendringRef?

    fun deleteByTotrinnskontrollId(totrinnskontrollId: UUID)
}

data class EnkeltplassPrisendringRef(
    val totrinnskontrollId: UUID,
    val gjennomforingId: UUID,
    val prismodellId: UUID,
)
