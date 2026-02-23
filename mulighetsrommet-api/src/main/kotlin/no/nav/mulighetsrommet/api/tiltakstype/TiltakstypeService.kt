package no.nav.mulighetsrommet.api.tiltakstype

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.tiltakstype.model.Tiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.UUID
import java.util.concurrent.TimeUnit

class TiltakstypeService(
    private val config: Config = Config(),
    private val db: ApiDatabase,
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

    fun getAll(filter: TiltakstypeFilter): List<TiltakstypeDto> = CacheUtils.tryCacheFirstNotNull(cacheByFilter, filter) {
        val tiltakskoder = config.features
            .filterValues { it.containsAll(filter.features) }
            .mapTo(mutableSetOf()) { it.key }

        db.session {
            queries.tiltakstype
                .getAll(
                    tiltakskoder = tiltakskoder,
                    sortering = filter.sortering,
                )
                .mapNotNull { it.toTiltakstypeDto() }
        }
    }

    fun getById(id: UUID): TiltakstypeDto? = db.session {
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

    private fun Tiltakstype.toTiltakstypeDto(): TiltakstypeDto? {
        val tiltakskode = tiltakskode ?: return null
        val features = config.features[tiltakskode] ?: setOf()
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
        )
    }
}
