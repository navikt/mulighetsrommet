package no.nav.mulighetsrommet.api.tiltakstype.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeFilter
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeVeilederinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.tiltakstype.model.Tiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeHandling
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeKompaktDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.utils.toUUID
import java.util.UUID

class TiltakstypeDetaljerService(
    private val db: ApiDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val sanityService: SanityService,
    private val navAnsattService: NavAnsattService,
) {

    suspend fun upsertRedaksjoneltInnhold(id: UUID, request: TiltakstypeVeilederinfoRequest): TiltakstypeDto? {
        db.transaction {
            queries.tiltakstype.upsertRedaksjoneltInnhold(id, request.beskrivelse, request.faneinnhold)
            queries.tiltakstype.setKanKombineresMed(id, request.kanKombineresMed)
            queries.tiltakstype.setFaglenker(id, request.faglenker)
        }
        return getById(id)
    }

    fun getHandlinger(id: UUID, navIdent: NavIdent): Set<TiltakstypeHandling> {
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return setOf()
        return setOfNotNull(
            TiltakstypeHandling.REDIGER_VEILEDERINFO.takeIf { ansatt.hasGenerellRolle(Rolle.TILTAKSTYPER_SKRIV) },
        )
    }

    fun getAll(filter: TiltakstypeFilter): List<TiltakstypeKompaktDto> {
        val tiltakskoder = tiltakstypeService.getTiltakskodeByFeatures(
            setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON),
        )

        val tiltakstyper = db.session {
            queries.tiltakstype.getAll(tiltakskoder = tiltakskoder, sortering = filter.sortering)
        }

        return tiltakstyper.mapNotNull { it.toTiltakstypeKompaktDto() }
    }

    suspend fun getById(id: UUID): TiltakstypeDto? {
        val tiltakstype = db.session { queries.tiltakstype.get(id) } ?: return null
        val tiltakskode = tiltakstype.tiltakskode ?: return null
        val features = tiltakstypeService.getFeatures(tiltakskode)

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
                faneinnhold = sanityTiltakstype?.faneinnhold,
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

        return TiltakstypeDto(
            id = tiltakstype.id,
            navn = tiltakstype.navn,
            tiltakskode = tiltakskode,
            startDato = tiltakstype.startDato,
            sluttDato = tiltakstype.sluttDato,
            status = tiltakstype.status,
            sanityId = tiltakstype.sanityId,
            features = features,
            egenskaper = tiltakskode.egenskaper,
            gruppe = tiltakskode.gruppe?.tittel,
            veilederinfo = veilederinfo,
        )
    }

    private fun Tiltakstype.toTiltakstypeKompaktDto(): TiltakstypeKompaktDto? {
        val tiltakskode = tiltakskode ?: return null
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

    private suspend fun getSanityTiltakstype(sanityId: UUID): SanityTiltakstype? {
        val sanityData = sanityService.getTiltakstyper().associateBy { it._id }
        return sanityData[sanityId.toString()]
    }
}
