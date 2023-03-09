package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.api.domain.dto.FylkeResponse
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class SanityService(private val config: Config, private val brukerService: BrukerService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient
    private val fylkenummerCache = mutableMapOf<String?, String>()
    private val sanityCache: Cache<String, SanityResponse> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    data class Config(
        val authToken: String?,
        val dataset: String,
        val projectId: String,
        val apiVersion: String = "v2023-01-01",
    ) {
        val apiUrl get() = "https://$projectId.apicdn.sanity.io/$apiVersion/data/query/$dataset"
    }

    init {
        logger.debug("Init SanityHttpClient")
        client = httpJsonClient().config {
            defaultRequest {
                config.authToken?.also {
                    bearerAuth(it)
                }

                url(config.apiUrl)
            }
        }
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("sanityCache", sanityCache)
    }

    suspend fun executeQuery(query: String, fnr: String?, accessToken: String): SanityResponse {
        if (fnr !== null) {
            return getMedBrukerdata(query, fnr, accessToken)
        }
        return get(query)
    }

    suspend fun executeQuery(query: String): SanityResponse {
        return get(query)
    }

    private suspend fun getMedBrukerdata(query: String, fnr: String, accessToken: String): SanityResponse {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.oppfolgingsenhet?.enhetId ?: ""
        val fylkesId = getFylkeIdBasertPaaEnhetsId(brukerData.oppfolgingsenhet?.enhetId) ?: ""
        return get(query, enhetsId, fylkesId)
    }

    private suspend fun get(query: String, enhetsId: String? = null, fylkeId: String? = null): SanityResponse {
        try {
            client.get {
                url {
                    parameters.append("query", query)
                    enhetsId?.let { parameters.append("\$enhetsId", "\"enhet.lokal.$it\"") }
                    fylkeId?.let { parameters.append("\$fylkeId", "\"enhet.fylke.$it\"") }
                }
            }.let {
                return it.body()
            }
        } catch (exception: Exception) {
            logger.error("Klarte ikke hente data fra Sanity", exception)
            return SanityResponse.Error(JsonNull.jsonObject)
        }
    }

    suspend fun hentInnsatsgrupper(): SanityResponse {
        return CacheUtils.tryCacheFirstNotNull(sanityCache, "innsatsgrupper") {
            executeQuery(
                """
            *[_type == "innsatsgruppe" && !(_id in path("drafts.**"))] | order(order asc)
                """.trimIndent()
            )
        }
    }

    suspend fun hentTiltakstyper(): SanityResponse {
        return CacheUtils.tryCacheFirstNotNull(sanityCache, "tiltakstyper") {
            executeQuery(
                """
                *[_type == "tiltakstype" && !(_id in path("drafts.**"))]
                """.trimIndent()
            )
        }
    }

    suspend fun hentLokasjonerForBrukersEnhetOgFylke(fnr: String, accessToken: String): SanityResponse {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.oppfolgingsenhet?.enhetId ?: ""
        val fylkeId = getFylkeIdBasertPaaEnhetsId(brukerData.oppfolgingsenhet?.enhetId) ?: ""

        val query = """
            array::unique(*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**"))
            ${byggEnhetOgFylkeFilter(enhetsId, fylkeId)}]
            {
              lokasjon
            }.lokasjon)
        """.trimIndent()

        return CacheUtils.tryCacheFirstNotNull(sanityCache, fnr) {
            executeQuery(
                query
            )
        }
    }

    suspend fun hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
        fnr: String,
        accessToken: String,
        filter: Tiltaksgjennomforingsfilter
    ): SanityResponse {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.oppfolgingsenhet?.enhetId ?: ""
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

        return executeQuery(query)
    }

    suspend fun hentTiltaksgjennomforing(id: String): SanityResponse {
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

        return executeQuery(query)
    }

    private suspend fun getFylkeIdBasertPaaEnhetsId(enhetsId: String?): String? {
        if (fylkenummerCache[enhetsId] != null) {
            return fylkenummerCache[enhetsId]
        }

        val response = get("*[_type == \"enhet\" && type == \"Lokal\" && nummer.current == \"$enhetsId\"][0]{fylke->}")

        logger.info("Henter data om fylkeskontor basert på enhetsId: '$enhetsId' - Response: {}", response)

        val fylkeResponse = when (response) {
            is SanityResponse.Result -> response.result?.let {
                JsonIgnoreUnknownKeys.decodeFromJsonElement<FylkeResponse>(
                    it
                )
            }

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
