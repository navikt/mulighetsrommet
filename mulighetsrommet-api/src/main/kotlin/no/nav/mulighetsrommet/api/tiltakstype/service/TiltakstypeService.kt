package no.nav.mulighetsrommet.api.tiltakstype.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utils.CacheUtils
import no.nav.mulighetsrommet.utils.toUUID
import java.util.UUID
import java.util.concurrent.TimeUnit

class TiltakstypeService(
    private val config: Config = Config(),
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val navAnsattService: NavAnsattService,
) {

    data class Config(
        val features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    )

    private val cacheByTiltakskode: Cache<String, Tiltakstype> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(200)
        .recordStats()
        .build()

    private val cacheByArenakode: Cache<String, List<Tiltakstype>> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(20)
        .recordStats()
        .build()

    private val cacheByFilter: Cache<TiltakstypeFilter, List<TiltakstypeDto>> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(20)
        .recordStats()
        .build()

    fun erMigrert(tiltakskode: Tiltakskode): Boolean {
        return config.features[tiltakskode].orEmpty().contains(TiltakstypeFeature.MIGRERT)
    }

    fun erUtfaset(tiltakskode: Tiltakskode): Boolean {
        return config.features[tiltakskode].orEmpty().contains(TiltakstypeFeature.UTFASET)
    }

    suspend fun getAll(
        filter: TiltakstypeFilter,
    ): List<TiltakstypeDto> = CacheUtils.tryCacheFirstNotNull(cacheByFilter, filter) {
        val tiltakskoder = config.features
            .filterValues { it.containsAll(filter.features) }
            .mapTo(mutableSetOf()) { it.key }

        val tiltakstyper = db.session {
            queries.tiltakstype.getAll(tiltakskoder = tiltakskoder, sortering = filter.sortering)
        }

        tiltakstyper.mapNotNull { it.toTiltakstypeDto() }
    }

    suspend fun getById(id: UUID): TiltakstypeDto? = db.session {
        queries.tiltakstype.get(id)?.toTiltakstypeDto()
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype {
        return CacheUtils.tryCacheFirstNotNull(cacheByTiltakskode, tiltakskode.name) {
            db.session { queries.tiltakstype.getByTiltakskode(tiltakskode) }
        }
    }

    fun getByArenaTiltakskode(arenaKode: String): List<Tiltakstype> {
        return CacheUtils.tryCacheFirstNotNull(cacheByArenakode, arenaKode) {
            db.session { queries.tiltakstype.getByArenaTiltakskode(arenaKode) }
        }
    }

    suspend fun upsertRedaksjoneltInnhold(id: UUID, request: TiltakstypeVeilederinfoRequest): TiltakstypeDto? {
        db.transaction {
            queries.tiltakstype.upsertRedaksjoneltInnhold(id, request.beskrivelse, request.faneinnhold)
            queries.tiltakstype.setKanKombineresMed(id, request.kanKombineresMed)
            queries.tiltakstype.setFaglenker(id, request.faglenker)
        }
        invalidateCaches()
        return getById(id)
    }

    fun getHandlinger(id: UUID, navIdent: NavIdent): Set<TiltakstypeHandling> {
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return setOf()
        return setOfNotNull(
            TiltakstypeHandling.REDIGER_VEILEDERINFO.takeIf {
                ansatt.hasGenerellRolle(Rolle.TILTAKSTYPER_SKRIV)
            },
        )
    }

    private fun invalidateCaches() {
        cacheByTiltakskode.invalidateAll()
        cacheByArenakode.invalidateAll()
        cacheByFilter.invalidateAll()
    }

    private suspend fun Tiltakstype.toTiltakstypeDto(): TiltakstypeDto? {
        val tiltakskode = tiltakskode ?: return null

        val features = config.features[tiltakskode] ?: setOf()

        val veilederinfo = if (features.contains(TiltakstypeFeature.MIGRERT_REDAKSJONELT_INNHOLD)) {
            veilederinfo
        } else {
            val sanityTiltakstype = sanityId?.let { getSanityTiltakstype(it) }
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
            id = id,
            navn = navn,
            tiltakskode = tiltakskode,
            startDato = startDato,
            sluttDato = sluttDato,
            status = status,
            sanityId = sanityId,
            features = features,
            egenskaper = tiltakskode.egenskaper,
            gruppe = tiltakskode.gruppe?.tittel,
            veilederinfo = veilederinfo,
        )
    }

    private suspend fun getSanityTiltakstype(sanityId: UUID): SanityTiltakstype? {
        val sanityData = sanityService.getTiltakstyper().associateBy { it._id }
        return sanityData[sanityId.toString()]
    }
}
