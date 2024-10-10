package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeDto
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.TiltakstypeFilter
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class TiltakstypeService(
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val enabledTiltakskoder: List<Tiltakskode>,
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
