package no.nav.mulighetsrommet.api.application.tiltak

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.application.AdminDatabase
import no.nav.mulighetsrommet.api.domain.tiltak.SortDirection
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeSortField
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID
import java.util.concurrent.TimeUnit

class TiltakstypeService(
    private val config: Config = Config(),
    private val db: AdminDatabase,
) {

    data class Config(
        val features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    )

    private val cacheByTiltakskode: Cache<String, Tiltakstype> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(100)
        .recordStats()
        .build()

    private val cacheByArenakode: Cache<String, List<Tiltakstype>> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(100)
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

    fun getIdsByFeatures(features: Set<TiltakstypeFeature>): List<UUID> {
        val tiltakskoder = getTiltakskodeByFeatures(features)
        return db.session {
            repository.tiltakstype.getAll(tiltakskoder).map { it.id }
        }
    }

    fun getIdsByTiltakskoder(tiltakskoder: List<Tiltakskode>): List<UUID> {
        if (tiltakskoder.isEmpty()) {
            return emptyList()
        }

        return db.session {
            repository.tiltakstype.getAll(tiltakskoder.toSet()).map { it.id }
        }
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype {
        return cacheByTiltakskode.get(tiltakskode.name) {
            db.session { repository.tiltakstype.getByTiltakskode(tiltakskode) }
        }
    }

    fun getAll(
        tiltakskoder: Set<Tiltakskode> = emptySet(),
        sortField: TiltakstypeSortField = TiltakstypeSortField.NAVN,
        sortDirection: SortDirection = SortDirection.ASC,
    ): List<Tiltakstype> = db.session {
        repository.tiltakstype.getAll(
            tiltakskoder = tiltakskoder,
            sortField = sortField,
            sortDirection = sortDirection,
        )
    }

    fun getAllByArenaTiltakskode(arenaTiltakskode: String): List<Tiltakstype> {
        return cacheByArenakode.get(arenaTiltakskode) {
            val tiltakskoder = Tiltakskode.entries.filter { it.arenakode == arenaTiltakskode }
            db.session { repository.tiltakstype.getAll(tiltakskoder = tiltakskoder.toSet()) }
        }
    }
}
