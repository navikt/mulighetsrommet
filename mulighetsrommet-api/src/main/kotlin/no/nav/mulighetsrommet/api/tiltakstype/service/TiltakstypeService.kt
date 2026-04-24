package no.nav.mulighetsrommet.api.tiltakstype.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.tiltakstype.model.Tiltakstype
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

    fun getFeatures(tiltakskode: Tiltakskode): Set<TiltakstypeFeature> {
        return config.features[tiltakskode].orEmpty()
    }

    fun isEnabled(tiltakskode: Tiltakskode, feature: TiltakstypeFeature): Boolean {
        return getFeatures(tiltakskode).contains(feature)
    }

    fun erMigrert(tiltakskode: Tiltakskode): Boolean {
        return isEnabled(tiltakskode, TiltakstypeFeature.MIGRERT)
    }

    fun erUtfaset(tiltakskode: Tiltakskode): Boolean {
        return isEnabled(tiltakskode, TiltakstypeFeature.UTFASET)
    }

    fun getTiltakskodeByFeatures(features: Set<TiltakstypeFeature>): Set<Tiltakskode> {
        return config.features
            .filterValues { it.containsAll(features) }
            .mapTo(mutableSetOf()) { it.key }
    }

    fun getAllIdsByFeatures(features: Set<TiltakstypeFeature>): List<UUID> {
        val tiltakskoder = getTiltakskodeByFeatures(features)
        return db.session {
            queries.tiltakstype.getAll(tiltakskoder = tiltakskoder).map { it.id }
        }
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
}
