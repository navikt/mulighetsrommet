package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.api.utils.TiltakstypeFilter
import no.nav.mulighetsrommet.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class TiltakstypeService(
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val arenakodeEnabledTiltakstyper: List<String>,
) {

    private val cacheBySanityId: Cache<UUID, TiltakstypeAdminDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    fun isEnabled(tiltakstypeArenakode: String) = arenakodeEnabledTiltakstyper.contains(tiltakstypeArenakode)

    fun getWithFilter(
        filter: TiltakstypeFilter,
        pagination: PaginationParams,
    ): PaginatedResponse<TiltakstypeAdminDto> {
        val (totalCount, items) = tiltakstypeRepository.getAllSkalMigreres(
            filter,
            pagination,
        )

        return PaginatedResponse.of(pagination, totalCount, items)
    }

    fun getById(id: UUID): TiltakstypeAdminDto? {
        return tiltakstypeRepository.get(id)
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeAdminDto? {
        return CacheUtils.tryCacheFirstNullable(cacheBySanityId, sanityId) {
            tiltakstypeRepository.getBySanityId(sanityId)
        }
    }

    fun kanRedigeres(
        tiltakstype: TiltakstypeAdminDto,
    ): Boolean {
        return (
            arenakodeEnabledTiltakstyper + listOf(
                "VASV",
                "ARBFORB",
            )
            ).contains(tiltakstype.arenaKode)
    }
}
