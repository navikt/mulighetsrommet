package no.nav.mulighetsrommet.api.tiltakstype

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class TiltakstypeService(
    private val db: ApiDatabase,
    /**
     * Alle kjent gruppetiltak har foreløpig blitt migrert.
     * Denne står fortsatt åpen for konfigurasjon for fremtidige tiltak (bl.a. IPS/AMS).
     */
    private val enabledTiltakskoder: List<Tiltakskode> = listOf(
        Tiltakskode.AVKLARING,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    ),
) {

    private val cacheBySanityId: Cache<UUID, TiltakstypeDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(200)
        .recordStats()
        .build()

    private val cacheByGjennomforingId: Cache<UUID, TiltakstypeDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    private val cacheByTiltakskode: Cache<String, TiltakstypeDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(200)
        .recordStats()
        .build()

    fun isEnabled(tiltakskode: Tiltakskode?) = enabledTiltakskoder.contains(tiltakskode)

    // TODO fjerne paginering?
    fun getAll(
        filter: TiltakstypeFilter,
        pagination: Pagination,
    ): PaginatedResponse<TiltakstypeDto> = db.session {
        val items = queries.tiltakstype.getAllSkalMigreres(
            sortering = filter.sortering,
        )

        PaginatedResponse.of(pagination, items.size, items)
    }

    // TODO inline?
    fun getById(id: UUID): TiltakstypeDto? = db.session {
        queries.tiltakstype.get(id)
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheBySanityId, sanityId) {
            db.session { queries.tiltakstype.getBySanityId(sanityId) }
        }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheByGjennomforingId, gjennomforingId) {
            db.session { queries.tiltakstype.getByGjennomforingId(gjennomforingId) }
        }
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheByTiltakskode, tiltakskode.name) {
            db.session { queries.tiltakstype.getByTiltakskode(tiltakskode) }
        }
    }

    fun getByArenaTiltakskode(arenaKode: String): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheByTiltakskode, arenaKode) {
            db.session { queries.tiltakstype.getByArenaTiltakskode(arenaKode) }
        }
    }
}
