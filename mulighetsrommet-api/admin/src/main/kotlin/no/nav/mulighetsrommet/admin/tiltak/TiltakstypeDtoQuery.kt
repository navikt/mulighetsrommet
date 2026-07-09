package no.nav.mulighetsrommet.admin.tiltak

import no.nav.mulighetsrommet.admin.AdminDatabase
import java.util.UUID

data class GetTiltakstepeDto(
    val id: UUID,
)

class TiltakstypeDtoQuery(
    private val db: AdminDatabase,
    private val tiltakstypeService: TiltakstypeService,
) {
    fun execute(query: GetTiltakstepeDto): TiltakstypeDto? = db.session {
        val tiltakstype = repository.tiltakstype.get(query.id) ?: return@session null
        val features = tiltakstypeService.getFeatures(tiltakstype.tiltakskode)
        val veilederinfo = queries.tiltakstype.getVeilederinfo(tiltakstype.id) ?: TiltakstypeVeilderinfo(
            beskrivelse = null,
            faneinnhold = null,
            faglenker = emptyList(),
            kanKombineresMed = emptyList(),
        )
        val deltakerinfo = queries.tiltakstype.getDeltakerregistreringInnhold(tiltakstype.id)
        TiltakstypeDto(
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
}
