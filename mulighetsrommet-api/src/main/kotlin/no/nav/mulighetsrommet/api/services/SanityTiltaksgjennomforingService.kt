package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.http.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class SanityTiltaksgjennomforingService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val avtaleRepository: AvtaleRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val sanityTiltakstypeIdCache: Cache<UUID, TiltakstypeIdResponse> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("sanityTiltakstypeIdCache", sanityTiltakstypeIdCache)
    }

    private suspend fun oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing: TiltaksgjennomforingAdminDto): Boolean {
        val tiltaksnummer = tiltaksgjennomforing.tiltaksnummer ?: return false

        val sanityTiltaksgjennomforinger = hentTiltaksgjennomforinger(tiltaksnummer)
        if (sanityTiltaksgjennomforinger.size > 1) {
            throw Exception("Fant ${sanityTiltaksgjennomforinger.size} sanity dokumenter med tiltaksnummer: $tiltaksnummer")
        }
        if (sanityTiltaksgjennomforinger.isEmpty()) {
            return false
        }
        tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
            tiltaksgjennomforing.id,
            UUID.fromString(sanityTiltaksgjennomforinger[0]._id),
        ).getOrThrow()
        return true
    }

    suspend fun opprettSanityTiltaksgjennomforing(
        tiltaksgjennomforing: TiltaksgjennomforingAdminDto,
        dryRun: Boolean = true,
    ) {
        if (tiltaksgjennomforing.sanityId != null || oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing)) {
            return
        }

        val sanityTiltaksgjennomforingId = UUID.randomUUID()
        val avtale = tiltaksgjennomforing.avtaleId?.let { avtaleRepository.get(it).getOrThrow() }

        val sanityTiltaksgjennomforing = SanityTiltaksgjennomforing(
            _id = sanityTiltaksgjennomforingId.toString(),
            tiltaksgjennomforingNavn = tiltaksgjennomforing.navn,
            enheter = tiltaksgjennomforing.navEnheter.map {
                EnhetRef(_ref = "enhet.lokal.$it", _key = it)
            },
            fylke = avtale?.navRegion?.enhetsnummer?.let {
                FylkeRef(_ref = "enhet.fylke.$it")
            },
            tiltakstype = hentTiltakstypeId(tiltaksgjennomforing.tiltakstype.id)?.let {
                TiltakstypeRef(_ref = it._id)
            },
        )

        val response = sanityClient.mutate(
            Mutations(mutations = listOf(Mutation(createIfNotExists = sanityTiltaksgjennomforing))),
            dryRun = dryRun,
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke opprette tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Opprettet tiltaksgjennomforing i Sanity med id: $sanityTiltaksgjennomforingId")
        }

        tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(tiltaksgjennomforing.id, sanityTiltaksgjennomforingId)
            .getOrThrow()
    }

    private suspend fun hentTiltakstypeId(tiltakstypeId: UUID): TiltakstypeIdResponse? {
        return CacheUtils.tryCacheFirstNotNull(sanityTiltakstypeIdCache, tiltakstypeId) {
            val query = """
                *[_type == "tiltakstype" &&
                !(_id in path('drafts.**')) &&
                tiltakstypeDbId == "$tiltakstypeId"]
                { _id }
            """.trimIndent()
            return when (val response = sanityClient.query(query)) {
                is SanityResponse.Result -> {
                    val ider = response.decode<List<TiltakstypeIdResponse>>()
                    if (ider.size > 1) {
                        throw RuntimeException("Fant flere tiltakstyper i Sanity på id: $tiltakstypeId")
                    }
                    ider.getOrNull(0)
                }

                is SanityResponse.Error -> {
                    throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
                }
            }
        }
    }

    private suspend fun hentTiltaksgjennomforinger(tiltaksnummer: String): List<SanityTiltaksgjennomforingResponse> {
        val query = """
            *[_type == "tiltaksgjennomforing" &&
            !(_id in path('drafts.**')) &&
            (tiltaksnummer.current == "$tiltaksnummer" || tiltaksnummer.current == "${tiltaksnummer.split("#").getOrNull(1)}")]
        """.trimIndent()
        return when (val response = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                response.decode()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }
    }
}
