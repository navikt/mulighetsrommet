package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.api.utils.SanityUtils.isUnderliggendeEnhet
import no.nav.mulighetsrommet.api.utils.SanityUtils.toEnhetId
import no.nav.mulighetsrommet.api.utils.SanityUtils.toStatus
import no.nav.mulighetsrommet.api.utils.SanityUtils.toType
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class SanityService(
    private val config: Config,
    private val brukerService: BrukerService,
    private val slackNotifier: SlackNotifier,
) {
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
        val authTokenForMutation: String?,
        val dataset: String,
        val projectId: String,
        val apiVersion: String = "v2023-01-01",
    ) {
        val apiUrl get() = "https://$projectId.apicdn.sanity.io/$apiVersion/data/query/$dataset"
        val mutateUrl get() = "https://$projectId.apicdn.sanity.io/$apiVersion/data/mutate/$dataset"
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

    private suspend fun executeQuery(query: String): SanityResponse {
        return get(query)
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
                """.trimIndent(),
            )
        }
    }

    suspend fun hentTiltakstyper(): SanityResponse {
        return CacheUtils.tryCacheFirstNotNull(sanityCache, "tiltakstyper") {
            executeQuery(
                """
                *[_type == "tiltakstype" && !(_id in path("drafts.**"))]
                """.trimIndent(),
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
                query,
            )
        }
    }

    suspend fun hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
        fnr: String,
        accessToken: String,
        filter: TiltaksgjennomforingFilter,
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
                    it,
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

    suspend fun updateEnheterToSanity(sanityEnheter: List<SanityEnhet>) {
        logger.info("Oppdaterer Sanity-enheter - Antall: ${sanityEnheter.size}")
        val mutations = Mutations(mutations = sanityEnheter.map { Mutation(createOrReplace = it) })
        val mutateClient = client.config {
            defaultRequest {
                config.authTokenForMutation?.also {
                    bearerAuth(it)
                }
                url(config.mutateUrl)
            }
        }
        val response = mutateClient.post {
            contentType(ContentType.Application.Json)
            setBody(mutations)
        }
        if (response.status !== HttpStatusCode.OK) {
            logger.error("Klarte ikke oppdatere enheter fra NORG til Sanity: {}", response.status)
            slackNotifier.sendMessage("Klarte ikke oppdatere enheter fra NORG til Sanity. Statuskode: ${response.status.value}. Dette må sees på av en utvikler.")
        } else {
            logger.info("Oppdaterte enheter fra NORG til Sanity.")
        }
    }

    fun spesialEnheterToSanityEnheter(enheter: List<Norg2Response>): List<SanityEnhet> {
        return enheter.filter { SanityUtils.relevanteStatuser(it.enhet.status) }.map { toSanityEnhet(it.enhet) }
    }

    fun fylkeOgUnderenheterToSanity(fylkerOgEnheter: List<Norg2Response>): List<SanityEnhet> {
        val fylker =
            fylkerOgEnheter.filter { SanityUtils.relevanteStatuser(it.enhet.status) && it.enhet.type == Norg2Type.FYLKE }

        return fylker.flatMap { fylke ->
            val underliggendeEnheter = fylkerOgEnheter.filter { isUnderliggendeEnhet(fylke.enhet, it) }
                .map { toSanityEnhet(it.enhet, fylke.enhet) }
            listOf(toSanityEnhet(fylke.enhet)) + underliggendeEnheter
        }
    }

    private fun toSanityEnhet(enhet: Norg2EnhetDto, fylke: Norg2EnhetDto? = null): SanityEnhet {
        var fylkeTilEnhet: FylkeRef? = null

        if (fylke != null) {
            fylkeTilEnhet = FylkeRef(
                _type = "reference",
                _ref = toEnhetId(fylke),
                _key = fylke.enhetNr,
            )
        } else if (enhet.type == Norg2Type.ALS) {
            val fylkesnummer = getFylkesnummerForSpesialenhet(enhet.enhetNr)
            if (fylkesnummer != null) {
                fylkeTilEnhet = FylkeRef(
                    _type = "reference",
                    _ref = "enhet.fylke.$fylkesnummer",
                    _key = fylkesnummer,
                )
            }
        }

        return SanityEnhet(
            _id = toEnhetId(enhet),
            _type = "enhet",
            navn = enhet.navn,
            nummer = EnhetSlug(
                _type = "slug",
                current = enhet.enhetNr,
            ),
            type = toType(enhet.type.name),
            status = toStatus(enhet.status.name),
            fylke = fylkeTilEnhet,
        )
    }

    private fun getFylkesnummerForSpesialenhet(enhetNr: String): String? {
        val spesialEnheterTilFylkeMap = mapOf(
            "1291" to "1200", // Vestland
            "0291" to "0200", // Øst-Viken
            "1591" to "1500", // Møre og Romsdal,
            "1891" to "1800", // Nordland
            "0491" to "0400", // Innlandet
            "0691" to "0600", // Vest-Viken,
            "0891" to "0800", // Vestfold og Telemark
            "1091" to "1000", // Agder,
            "1991" to "1900", // Troms og Finnmark
            "5772" to "5700", // Trøndelag,
            "0391" to "0300", // Oslo
            "1191" to "1100", // Rogaland
        )

        val fantFylke = spesialEnheterTilFylkeMap[enhetNr]
        if (fantFylke == null) {
            slackNotifier.sendMessage("Fant ikke fylke for spesialenhet med enhetsnummer: $enhetNr. En utvikler må sjekke om enheten skal mappe til et fylke.")
            return null
        }
        return fantFylke
    }
}
