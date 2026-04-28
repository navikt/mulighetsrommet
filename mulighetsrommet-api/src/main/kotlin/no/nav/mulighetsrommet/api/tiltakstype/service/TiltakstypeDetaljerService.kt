package no.nav.mulighetsrommet.api.tiltakstype.service

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeDeltakerinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeFilter
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeVeilederinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.model.Tiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeHandling
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeKompaktDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeSystem
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import java.util.UUID

class TiltakstypeDetaljerService(
    private val config: Config,
    private val db: ApiDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val topic: String,
    )

    fun upsertVeilederinfo(
        id: UUID,
        request: TiltakstypeVeilederinfoRequest,
    ): TiltakstypeDto? {
        db.transaction {
            queries.tiltakstype.upsertRedaksjoneltInnhold(id, request.beskrivelse, request.faneinnhold)
            queries.tiltakstype.setKanKombineresMed(id, request.kanKombineresMed)
            queries.tiltakstype.setFaglenker(id, request.faglenker)
        }
        return getById(id)
    }

    fun upsertDeltakerinfo(
        id: UUID,
        request: TiltakstypeDeltakerinfoRequest,
    ): TiltakstypeDto? {
        db.transaction {
            queries.tiltakstype.upsertDeltakerRegistreringInnhold(id, request.ledetekst, request.innholdskoder)
            publishToKafka(id)
        }
        return getById(id)
    }

    fun getHandlinger(id: UUID, navIdent: NavIdent): Set<TiltakstypeHandling> {
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return setOf()
        return setOfNotNull(
            TiltakstypeHandling.REDIGER_VEILEDERINFO.takeIf { ansatt.hasGenerellRolle(Rolle.TILTAKSTYPER_SKRIV) },
            TiltakstypeHandling.REDIGER_DELTAKERINFO.takeIf { ansatt.hasGenerellRolle(Rolle.TILTAKSTYPER_SKRIV) },
        )
    }

    fun getAll(filter: TiltakstypeFilter): List<TiltakstypeKompaktDto> {
        val tiltakskoder = if (filter.egenskaper.isNotEmpty()) {
            Tiltakskode.entries.filter { kode -> kode.egenskaper.any { it in filter.egenskaper } }.toSet()
        } else {
            setOf()
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
        val tiltakstype = queries.tiltakstype.get(id) ?: return null

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

    private fun QueryContext.publishToKafka(id: UUID) {
        val ekstern = requireNotNull(queries.tiltakstype.getEksternTiltakstype(id)) {
            "Klarte ikke hente ekstern tiltakstype for id $id"
        }

        if (ekstern.tiltakskode.system != TiltakstypeSystem.TILTAKSADMINISTRASJON) {
            return
        }

        val record = StoredProducerRecord(
            config.topic,
            ekstern.id.toString().toByteArray(),
            Json.encodeToString(TiltakstypeV3Dto.serializer(), ekstern).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}
