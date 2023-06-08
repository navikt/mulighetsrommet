package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.FylkeResponse
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltaksgjennomforing
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class VeilederflateSanityService(
    private val sanityClient: SanityClient,
    private val brukerService: BrukerService,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fylkenummerCache = mutableMapOf<String?, String>()
    private val sanityCache: Cache<String, SanityResponse> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("sanityCache", sanityCache)
    }

    suspend fun hentInnsatsgrupper(): SanityResponse {
        return CacheUtils.tryCacheFirstNotNull(sanityCache, "innsatsgrupper") {
            sanityClient.query(
                """
                *[_type == "innsatsgruppe" && !(_id in path("drafts.**"))] | order(order asc)
                """.trimIndent(),
            )
        }
    }

    suspend fun hentTiltakstyper(): SanityResponse {
        return CacheUtils.tryCacheFirstNotNull(sanityCache, "tiltakstyper") {
            sanityClient.query(
                """
                    *[_type == "tiltakstype" && !(_id in path("drafts.**"))]
                """.trimIndent(),
            )
        }
    }

    suspend fun hentLokasjonerForBrukersEnhetOgFylke(fnr: String, accessToken: String): SanityResponse {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.geografiskEnhet?.enhetsnummer ?: ""
        val fylkeId = getFylkeIdBasertPaaEnhetsId(enhetsId) ?: ""

        val query = """
            array::unique(*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**"))
            ${byggEnhetOgFylkeFilter(enhetsId, fylkeId)}]
            {
              lokasjon
            }.lokasjon)
        """.trimIndent()

        return CacheUtils.tryCacheFirstNotNull(sanityCache, fnr) {
            sanityClient.query(query)
        }
    }

    suspend fun hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
        fnr: String,
        accessToken: String,
        filter: TiltaksgjennomforingFilter,
    ): List<VeilederflateTiltaksgjennomforing> {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.geografiskEnhet?.enhetsnummer ?: ""
        val fylkeId = getFylkeIdBasertPaaEnhetsId(enhetsId) ?: ""
        val query = """
            *[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**"))
              ${byggInnsatsgruppeFilter(filter.innsatsgruppe)}
              ${byggTiltakstypeFilter(filter.tiltakstypeIder)}
              ${byggSokeFilter(filter.sokestreng)}
              ${byggLokasjonsFilter(filter.lokasjoner)}
              ${byggEnhetOgFylkeFilter(enhetsId, fylkeId)}
              ]
              {
                _id,
                tiltaksgjennomforingNavn,
                lokasjon,
                oppstart,
                oppstartsdato,
                estimert_ventetid,
                "tiltaksnummer": tiltaksnummer.current,
                kontaktinfoArrangor->{selskapsnavn},
                tiltakstype->{tiltakstypeNavn},
                tilgjengelighetsstatus
              }
        """.trimIndent()

        return when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                val gjennomforinger = result.decode<List<VeilederflateTiltaksgjennomforing>>()
                supplerDataFraDB(gjennomforinger)
            }
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun hentTiltaksgjennomforing(id: String): List<VeilederflateTiltaksgjennomforing> {
        val query = """
            *[_type == "tiltaksgjennomforing" && (_id == '$id' || _id == 'drafts.$id')] {
                _id,
                tiltaksgjennomforingNavn,
                beskrivelse,
                "tiltaksnummer": tiltaksnummer.current,
                tilgjengelighetsstatus,
                estimert_ventetid,
                lokasjon,
                oppstart,
                oppstartsdato,
                sluttdato,
                faneinnhold {
                  forHvemInfoboks,
                  forHvem,
                  detaljerOgInnholdInfoboks,
                  detaljerOgInnhold,
                  pameldingOgVarighetInfoboks,
                  pameldingOgVarighet,
                },
                kontaktinfoArrangor->,
                kontaktinfoTiltaksansvarlige[]->,
                tiltakstype->{
                  ...,
                  regelverkFiler[]-> {
                    _id,
                    "regelverkFilUrl": regelverkFilOpplastning.asset->url,
                    regelverkFilNavn
                  },
                  regelverkLenker[]->,
                  innsatsgruppe->,
                }
              }
        """.trimIndent()

        return when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                val gjennomforinger = result.decode<List<VeilederflateTiltaksgjennomforing>>()
                supplerDataFraDB(gjennomforinger)
            }
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private fun supplerDataFraDB(gjennomforinger: List<VeilederflateTiltaksgjennomforing>): List<VeilederflateTiltaksgjennomforing> {
        val sanityIds = gjennomforinger
            .mapNotNull {
                try {
                    UUID.fromString(it._id)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

        val gjennomforingerFraDb = tiltaksgjennomforingService.getBySanitIds(sanityIds)

        return gjennomforinger
            .map {
                val apiGjennomforing = gjennomforingerFraDb[it._id]
                it.copy(
                    stengtFra = apiGjennomforing?.stengtFra,
                    stengtTil = apiGjennomforing?.stengtTil,
                )
            }
    }

    private suspend fun getFylkeIdBasertPaaEnhetsId(enhetsId: String?): String? {
        if (fylkenummerCache[enhetsId] != null) {
            return fylkenummerCache[enhetsId]
        }

        val response =
            sanityClient.query(query = "*[_type == \"enhet\" && type == \"Lokal\" && nummer.current == \"$enhetsId\"][0]{fylke->}")

        logger.info("Henter data om fylkeskontor basert på enhetsId: '$enhetsId' - Response: {}", response)

        val fylkeResponse = when (response) {
            is SanityResponse.Result -> response.decode<FylkeResponse>()

            else -> null
        }

        return try {
            val fylkeNummer = fylkeResponse?.fylke?.nummer?.current
            if (fylkeNummer != null && enhetsId != null) {
                fylkenummerCache[enhetsId] = fylkeNummer
            }
            fylkeNummer
        } catch (exception: Throwable) {
            logger.warn("Spørring mot Sanity feilet", exception)
            null
        }
    }
}
