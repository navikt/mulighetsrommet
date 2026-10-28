package no.nav.mulighetsrommet.api.persistence.gjennomforing

import arrow.core.Either
import no.nav.mulighetsrommet.admin.gjennomforing.EnkeltplassPrisendringHandler
import no.nav.mulighetsrommet.admin.gjennomforing.EnkeltplassPrisendringRef
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingWriter
import no.nav.mulighetsrommet.admin.gjennomforing.SetGjennomforingArenaData
import no.nav.mulighetsrommet.admin.gjennomforing.SetGjennomforingKontaktperson
import no.nav.mulighetsrommet.admin.gjennomforing.StengtPeriodeOverlapper
import no.nav.mulighetsrommet.admin.gjennomforing.UpdateGjennomforingDetaljer
import no.nav.mulighetsrommet.admin.gjennomforing.UpsertGjennomforing
import no.nav.mulighetsrommet.api.domain.gjennomforing.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

/**
 * Implementerer skriveporten fra admin ved å mappe kommandoene til de eksisterende Dbo-ene.
 */
class SqlGjennomforingWriter(private val queries: GjennomforingQueries) : GjennomforingWriter {
    override fun upsert(gjennomforing: UpsertGjennomforing) {
        queries.upsert(
            GjennomforingDbo(
                id = gjennomforing.id,
                type = gjennomforing.type,
                tiltakskode = gjennomforing.tiltakskode,
                arrangorId = gjennomforing.arrangorId,
                navn = gjennomforing.navn,
                startDato = gjennomforing.startDato,
                sluttDato = gjennomforing.sluttDato,
                status = gjennomforing.status,
                deltidsprosent = gjennomforing.deltidsprosent,
                antallPlasser = gjennomforing.antallPlasser,
                oppstart = gjennomforing.oppstart,
                pameldingType = gjennomforing.pameldingType,
                avtaleId = gjennomforing.avtaleId,
                prismodellId = gjennomforing.prismodellId,
                ansvarligEnhet = gjennomforing.ansvarligEnhet,
                arenaTiltaksnummer = gjennomforing.arenaTiltaksnummer,
                arenaAnsvarligEnhet = gjennomforing.arenaAnsvarligEnhet,
            ),
        )
    }

    override fun updateDetaljer(detaljer: UpdateGjennomforingDetaljer) {
        queries.updateDetaljer(
            GjennomforingDetaljerDbo(
                id = detaljer.id,
                arrangorId = detaljer.arrangorId,
                oppstart = detaljer.oppstart,
                pameldingType = detaljer.pameldingType,
                navn = detaljer.navn,
                startDato = detaljer.startDato,
                sluttDato = detaljer.sluttDato,
                status = detaljer.status,
                deltidsprosent = detaljer.deltidsprosent,
                antallPlasser = detaljer.antallPlasser,
                prismodellId = detaljer.prismodellId,
                oppmoteSted = detaljer.oppmoteSted,
                tilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato,
                ansvarligEnhet = detaljer.ansvarligEnhet,
            ),
        )
    }

    override fun setEstimertVentetid(id: UUID, verdi: Int?, enhet: String?) {
        queries.setEstimertVentetid(id, verdi, enhet)
    }

    override fun setRedaksjoneltInnhold(id: UUID, beskrivelse: String?, faneinnhold: Faneinnhold?) {
        queries.setRedaksjoneltInnhold(id, beskrivelse, faneinnhold)
    }

    override fun setNavEnheter(id: UUID, navEnheter: Set<NavEnhetNummer>) {
        queries.setNavEnheter(id, navEnheter)
    }

    override fun setAdministratorer(id: UUID, administratorer: Set<NavIdent>) {
        queries.setAdministratorer(id, administratorer)
    }

    override fun setKontaktpersoner(id: UUID, kontaktpersoner: Set<SetGjennomforingKontaktperson>) {
        queries.setKontaktpersoner(
            id,
            kontaktpersoner.mapTo(linkedSetOf()) { GjennomforingKontaktpersonDbo(it.navIdent, it.beskrivelse) },
        )
    }

    override fun setArrangorKontaktpersoner(id: UUID, arrangorKontaktpersoner: Set<UUID>) {
        queries.setArrangorKontaktpersoner(id, arrangorKontaktpersoner)
    }

    override fun setArenaData(arenaData: SetGjennomforingArenaData) {
        queries.setArenaData(
            GjennomforingArenaDataDbo(
                id = arenaData.id,
                tiltaksnummer = arenaData.tiltaksnummer,
                arenaAnsvarligEnhet = arenaData.arenaAnsvarligEnhet,
            ),
        )
    }

    override fun setPrismodellId(gjennomforingId: UUID, prismodellId: UUID) {
        queries.setPrismodellId(gjennomforingId, prismodellId)
    }

    override fun setFreeTextSearch(id: UUID, content: List<String>) {
        queries.setFreeTextSearch(id, content)
    }

    override fun setPublisert(id: UUID, publisert: Boolean): Int {
        return queries.setPublisert(id, publisert)
    }

    override fun setApentForPamelding(id: UUID, apentForPamelding: Boolean): Int {
        return queries.setApentForPamelding(id, apentForPamelding)
    }

    override fun setTilgjengeligForArrangorDato(id: UUID, date: LocalDate): Int {
        return queries.setTilgjengeligForArrangorDato(id, date)
    }

    override fun setStatus(
        id: UUID,
        status: GjennomforingStatusType,
        sluttDato: LocalDate?,
        aarsaker: List<AvbrytGjennomforingAarsak>?,
        forklaring: String?,
    ): Int {
        return queries.setStatus(id, status, sluttDato, aarsaker, forklaring)
    }

    override fun frikobleKontaktpersonFraGjennomforing(kontaktpersonId: UUID, gjennomforingId: UUID) {
        queries.frikobleKontaktpersonFraGjennomforing(kontaktpersonId, gjennomforingId)
    }

    override fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
    ): Either<StengtPeriodeOverlapper, Unit> {
        return query {
            queries.setStengtHosArrangor(id, periode, beskrivelse)
        }.mapLeft {
            if (it is IntegrityConstraintViolation.ExclusionViolation) {
                StengtPeriodeOverlapper
            } else {
                throw it.error
            }
        }
    }

    override fun deleteStengtHosArrangor(id: Int) {
        queries.deleteStengtHosArrangor(id)
    }

    override fun aquireLock(id: UUID) {
        queries.aquireLock(id)
    }
}

class SqlEnkeltplassPrisendringHandler(
    private val queries: EnkeltplassPrisendringQueries,
) : EnkeltplassPrisendringHandler {
    override fun insert(prisendring: EnkeltplassPrisendringRef) {
        queries.insert(
            EnkeltplassPrisendringDbo(
                totrinnskontrollId = prisendring.totrinnskontrollId,
                gjennomforingId = prisendring.gjennomforingId,
                prismodellId = prisendring.prismodellId,
            ),
        )
    }

    override fun getByGjennomforingId(gjennomforingId: UUID): EnkeltplassPrisendringRef? {
        return queries.getByGjennomforingId(gjennomforingId)?.let {
            EnkeltplassPrisendringRef(
                totrinnskontrollId = it.totrinnskontrollId,
                gjennomforingId = it.gjennomforingId,
                prismodellId = it.prismodellId,
            )
        }
    }

    override fun deleteByTotrinnskontrollId(totrinnskontrollId: UUID) {
        queries.deleteByTotrinnskontrollId(totrinnskontrollId)
    }
}
