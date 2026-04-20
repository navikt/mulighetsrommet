package no.nav.mulighetsrommet.api.tiltakstype.service

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeDeltakerinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeFilter
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeVeilederinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.tiltakstype.model.Tiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeHandling
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeKompaktDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltakstypeSystem
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import no.nav.mulighetsrommet.utils.toUUID
import java.util.UUID

class TiltakstypeDetaljerService(
    private val config: Config,
    private val db: ApiDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val sanityService: SanityService,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val topic: String,
    )

    suspend fun upsertVeilederinfo(
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

    suspend fun upsertDeltakerinfo(
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
        val tiltakstyper = db.session {
            queries.tiltakstype.getAll(sortering = filter.sortering)
        }

        return tiltakstyper.map { it.toTiltakstypeKompaktDto() }
    }

    suspend fun getById(id: UUID): TiltakstypeDto? {
        val tiltakstype = db.session { queries.tiltakstype.get(id) } ?: return null

        val features = tiltakstypeService.getFeatures(tiltakstype.tiltakskode)
        val veilederinfo = if (features.contains(TiltakstypeFeature.MIGRERT_REDAKSJONELT_INNHOLD)) {
            db.session { queries.tiltakstype.getVeilederinfo(id) } ?: TiltakstypeVeilderinfo(
                beskrivelse = null,
                faneinnhold = null,
                faglenker = emptyList(),
                kanKombineresMed = emptyList(),
            )
        } else {
            val sanityTiltakstype = tiltakstype.sanityId?.let { getSanityTiltakstype(it) }
            TiltakstypeVeilderinfo(
                beskrivelse = sanityTiltakstype?.beskrivelse,
                faneinnhold = sanityTiltakstype?.faneinnhold?.copy(
                    delMedBruker = sanityTiltakstype.delingMedBruker,
                ),
                faglenker = sanityTiltakstype?.regelverkLenker?.mapNotNull { lenke ->
                    lenke.regelverkUrl?.let { url ->
                        RedaksjoneltInnholdLenke(
                            id = lenke._id!!.toUUID(),
                            url = url,
                            navn = lenke.regelverkLenkeNavn,
                            beskrivelse = lenke.beskrivelse,
                        )
                    }
                } ?: emptyList(),
                kanKombineresMed = sanityTiltakstype?.kanKombineresMed ?: emptyList(),
            )
        }

        val deltakerinfo = db.session { queries.tiltakstype.getDeltakerregistreringInnhold(id) }

        return TiltakstypeDto(
            id = tiltakstype.id,
            navn = tiltakstype.navn,
            tiltakskode = tiltakstype.tiltakskode,
            startDato = tiltakstype.startDato,
            sluttDato = tiltakstype.sluttDato,
            status = tiltakstype.status,
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
            startDato = startDato,
            sluttDato = sluttDato,
            status = status,
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

    private suspend fun getSanityTiltakstype(sanityId: UUID): SanityTiltakstype? {
        val sanityData = sanityService.getTiltakstyper().associateBy { it._id }
        return sanityData[sanityId.toString()]
    }
}
