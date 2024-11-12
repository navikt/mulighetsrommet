package no.nav.mulighetsrommet.api.tiltakstype

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class TiltakstypeService(
    private val tiltakstypeRepository: TiltakstypeRepository,
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
        .maximumSize(500)
        .recordStats()
        .build()

    private val cacheByGjennomforingId: Cache<UUID, TiltakstypeDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    fun isEnabled(tiltakskode: Tiltakskode?) = enabledTiltakskoder.contains(tiltakskode)

    fun getAll(
        filter: TiltakstypeFilter,
        pagination: Pagination,
    ): PaginatedResponse<TiltakstypeDto> {
        val (totalCount, items) = tiltakstypeRepository.getAllSkalMigreres(
            pagination = pagination,
            sortering = filter.sortering,
        )

        return PaginatedResponse.of(pagination, totalCount, items)
    }

    fun getById(id: UUID): TiltakstypeDto? {
        return tiltakstypeRepository.get(id)
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheBySanityId, sanityId) {
            tiltakstypeRepository.getBySanityId(sanityId)
        }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheByGjennomforingId, gjennomforingId) {
            tiltakstypeRepository.getByGjennomforingId(gjennomforingId)
        }
    }
}
