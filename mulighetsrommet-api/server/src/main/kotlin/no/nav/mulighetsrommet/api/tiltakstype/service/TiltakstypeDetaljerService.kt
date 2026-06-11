package no.nav.mulighetsrommet.api.tiltakstype.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeDeltakerinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeFilter
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeVeilederinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeHandling
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeKompaktDto
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltakstypeSystem
import java.time.LocalDateTime
import java.util.UUID

class TiltakstypeDetaljerService(
    private val db: ApiDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val navAnsattService: NavAnsattService,
) {
    fun upsertVeilederinfo(
        id: UUID,
        request: TiltakstypeVeilederinfoRequest,
        navIdent: NavIdent,
    ): TiltakstypeDto? = db.transaction {
        repository.tiltakstype.get(id) ?: return null

        queries.tiltakstype.upsertRedaksjoneltInnhold(id, request.beskrivelse, request.faneinnhold)
        queries.tiltakstype.setKanKombineresMed(id, request.kanKombineresMed)
        queries.tiltakstype.setFaglenker(id, request.faglenker)

        logEndring("Redigerte informasjon for veiledere", id, navIdent)
    }

    fun upsertDeltakerinfo(
        id: UUID,
        request: TiltakstypeDeltakerinfoRequest,
        navIdent: NavIdent,
    ): TiltakstypeDto? = db.transaction {
        repository.tiltakstype.get(id) ?: return null

        queries.tiltakstype.upsertDeltakerRegistreringInnhold(id, request.ledetekst, request.innholdskoder)
        publishToKafka(id)

        logEndring("Redigerte informasjon for deltakere", id, navIdent)
    }

    fun getHandlinger(id: UUID, navIdent: NavIdent): Set<TiltakstypeHandling> {
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return setOf()
        return setOfNotNull(
            TiltakstypeHandling.REDIGER_VEILEDERINFO.takeIf { ansatt.hasGenerellRolle(Rolle.TILTAKSTYPER_SKRIV) },
            TiltakstypeHandling.REDIGER_DELTAKERINFO.takeIf { ansatt.hasGenerellRolle(Rolle.TILTAKSTYPER_REDIGER_DELTAKERINFO) },
        )
    }

    fun getAll(filter: TiltakstypeFilter): List<TiltakstypeKompaktDto> {
        val enabled = tiltakstypeService
            .getTiltakskodeByFeatures(setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON))
            .ifEmpty { return listOf() }

        val tiltakskoder = if (filter.egenskaper.isNotEmpty()) {
            enabled
                .filter { kode -> kode.egenskaper.any { it in filter.egenskaper } }
                .toSet()
                .ifEmpty { return listOf() }
        } else {
            enabled
        }

        val tiltakstyper = db.session {
            queries.tiltakstype.getAll(
                tiltakskoder = tiltakskoder,
                sortField = filter.sortField,
                sortDirection = filter.sortDirection,
            )
        }

        return tiltakstyper.map { it.toTiltakstypeKompaktDto() }
    }

    fun getById(id: UUID): TiltakstypeDto? = db.session {
        getTiltakstypeDto(id)
    }

    fun getAllInnholdselementer(): List<Innholdselement> {
        return db.session { queries.tiltakstype.getAllInnholdselementer() }
    }

    private fun Tiltakstype.toTiltakstypeKompaktDto(): TiltakstypeKompaktDto {
        val features = tiltakstypeService.getFeatures(tiltakskode)
        return TiltakstypeKompaktDto(
            id = id,
            navn = navn,
            tiltakskode = tiltakskode,
            gruppe = tiltakskode.gruppe?.tittel,
            features = features,
            egenskaper = tiltakskode.egenskaper,
        )
    }

    private fun QueryContext.getTiltakstypeDto(id: UUID): TiltakstypeDto? {
        val tiltakstype = repository.tiltakstype.get(id) ?: return null
        val features = tiltakstypeService.getFeatures(tiltakstype.tiltakskode)
        val veilederinfo = queries.tiltakstype.getVeilederinfo(id) ?: TiltakstypeVeilderinfo(
            beskrivelse = null,
            faneinnhold = null,
            faglenker = emptyList(),
            kanKombineresMed = emptyList(),
        )
        val deltakerinfo = queries.tiltakstype.getDeltakerregistreringInnhold(id)
        return TiltakstypeDto(
            id = tiltakstype.id,
            navn = tiltakstype.navn,
            tiltakskode = tiltakstype.tiltakskode,
            sanityId = tiltakstype.sanityId,
            features = features,
            egenskaper = tiltakstype.tiltakskode.egenskaper,
            gruppe = tiltakstype.tiltakskode.gruppe?.tittel,
            veilederinfo = veilederinfo,
            deltakerinfo = deltakerinfo,
        )
    }

    private fun QueryContext.logEndring(
        operation: String,
        tiltakstypeId: UUID,
        endretAv: NavIdent,
    ): TiltakstypeDto {
        val dto = checkNotNull(getTiltakstypeDto(tiltakstypeId)) {
            "Tiltakstype med id $tiltakstypeId finnes ikke"
        }
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.TILTAKSTYPE,
            operation,
            endretAv,
            tiltakstypeId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
        return dto
    }

    private fun QueryContext.publishToKafka(id: UUID) {
        val ekstern = requireNotNull(queries.tiltakstype.getEksternTiltakstype(id)) {
            "Klarte ikke hente ekstern tiltakstype for id $id"
        }

        if (ekstern.tiltakskode.system != TiltakstypeSystem.TILTAKSADMINISTRASJON) {
            return
        }

        outbox.publish(ekstern)
    }
}
