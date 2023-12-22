package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.api.utils.TiltakstypeFilter
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class TiltakstypeService(private val tiltakstypeRepository: TiltakstypeRepository) {

    private val cacheBySanityId: Cache<UUID, TiltakstypeDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    fun getWithFilter(
        filter: TiltakstypeFilter,
        pagination: PaginationParams,
    ): PaginatedResponse<TiltakstypeDto> {
        val (totalCount, items) = tiltakstypeRepository.getAllSkalMigreres(
            filter,
            pagination,
        )

        return PaginatedResponse.of(pagination, totalCount, items)
    }

    fun getById(id: UUID): TiltakstypeDto? {
        return tiltakstypeRepository.get(id)
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeDto? {
        return CacheUtils.tryCacheFirstNullable(cacheBySanityId, sanityId) {
            tiltakstypeRepository.getBySanityId(sanityId)
        }
    }
}
