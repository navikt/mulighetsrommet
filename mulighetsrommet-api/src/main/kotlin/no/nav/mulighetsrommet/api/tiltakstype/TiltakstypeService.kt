package no.nav.mulighetsrommet.api.tiltakstype

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltakskoder.OpplaeringsTiltak2025
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.UUID
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

    private val cacheByTiltakskode: Cache<String, TiltakstypeDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(200)
        .recordStats()
        .build()

    private val cacheByArenakode: Cache<String, List<TiltakstypeDto>> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(20)
        .recordStats()
        .build()

    fun isEnabled(tiltakskode: Tiltakskode?) = enabledTiltakskoder.contains(tiltakskode)

    fun getAllGruppetiltak(filter: TiltakstypeFilter, inkluderOpplareing2025: Boolean? = false): List<TiltakstypeDto> =
        db.session {
            val standardGruppetiltak = setOf(
                Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                Tiltakskode.ARBEIDSRETTET_REHABILITERING,
                Tiltakskode.AVKLARING,
                Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
                Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
                Tiltakskode.OPPFOLGING,
                Tiltakskode.JOBBKLUBB,
                Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            )
            val tiltakskoder = if (inkluderOpplareing2025 == true) {
                standardGruppetiltak + OpplaeringsTiltak2025
            } else standardGruppetiltak

            val result = queries.tiltakstype.getAll(
                tiltakskoder = tiltakskoder,
                sortering = filter.sortering,
            )

            return result.filter {
                if (it.tiltakskode in OpplaeringsTiltak2025) {
                    it.sanityId != null
                }
                true
            }
        }

    fun getById(id: UUID): TiltakstypeDto? = db.session {
        queries.tiltakstype.get(id)
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheBySanityId, sanityId) {
            db.session { queries.tiltakstype.getBySanityId(sanityId) }
        }
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): TiltakstypeDto {
        return CacheUtils.tryCacheFirstNotNull(cacheByTiltakskode, tiltakskode.name) {
            db.session { queries.tiltakstype.getByTiltakskode(tiltakskode) }
        }
    }

    fun getByArenaTiltakskode(arenaKode: String): List<TiltakstypeDto> {
        return CacheUtils.tryCacheFirstNotNull(cacheByArenakode, arenaKode) {
            db.session { queries.tiltakstype.getByArenaTiltakskode(arenaKode) }
        }
    }
}
