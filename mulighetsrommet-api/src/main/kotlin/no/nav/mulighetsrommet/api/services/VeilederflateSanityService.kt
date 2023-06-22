package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.FylkeResponse
import no.nav.mulighetsrommet.api.domain.dto.KontaktinfoTiltaksansvarlige
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltaksgjennomforing
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class VeilederflateSanityService(
    private val sanityClient: SanityClient,
    private val brukerService: BrukerService,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
    private val navAnsattService: NavAnsattService,
    private val navEnhetService: NavEnhetService,
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
                "tiltaksnummer": tiltaksnummer.current,
                kontaktinfoArrangor->{selskapsnavn},
                tiltakstype->{tiltakstypeNavn}
              }
        """.trimIndent()

        return when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                val gjennomforinger = result.decode<List<VeilederflateTiltaksgjennomforing>>()
                supplerDataFraDB(gjennomforinger, enhetsId)
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun hentTiltaksgjennomforing(
        id: String,
        fnr: String,
        accessToken: String,
    ): List<VeilederflateTiltaksgjennomforing> {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.geografiskEnhet?.enhetsnummer ?: ""
        val query = """
            *[_type == "tiltaksgjennomforing" && (_id == '$id' || _id == 'drafts.$id')] {
                _id,
                tiltaksgjennomforingNavn,
                beskrivelse,
                "tiltaksnummer": tiltaksnummer.current,
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
                supplerDataFraDB(gjennomforinger, enhetsId)
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private fun supplerDataFraDB(
        gjennomforingerFraSanity: List<VeilederflateTiltaksgjennomforing>,
        enhetsId: String,
    ): List<VeilederflateTiltaksgjennomforing> {
        val sanityIds = gjennomforingerFraSanity
            .mapNotNull {
                try {
                    UUID.fromString(it._id)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

        val gjennomforingerFraDb = tiltaksgjennomforingService.getBySanityIds(sanityIds)

        return gjennomforingerFraSanity
            .map { sanityData ->
                val apiGjennomforing = gjennomforingerFraDb[sanityData._id]
                val kontaktpersoner = apiGjennomforing?.let { hentKontaktpersoner(it, enhetsId) } ?: emptyList()
                val oppstart = apiGjennomforing?.oppstart?.name?.lowercase() ?: sanityData.oppstart
                val oppstartsdato = apiGjennomforing?.oppstartsdato ?: sanityData.oppstartsdato
                sanityData.copy(
                    stengtFra = apiGjennomforing?.stengtFra,
                    stengtTil = apiGjennomforing?.stengtTil,
                    kontaktinfoTiltaksansvarlige = kontaktpersoner.ifEmpty { sanityData.kontaktinfoTiltaksansvarlige },
                    oppstart = oppstart,
                    oppstartsdato = oppstartsdato,
                    tilgjengelighetsstatus = apiGjennomforing?.tilgjengelighet?.name,
                    estimert_ventetid = apiGjennomforing?.estimertVentetid,
                )
            }
    }

    private fun hentKontaktpersoner(
        tiltaksgjennomforingAdminDto: TiltaksgjennomforingAdminDto,
        enhetsId: String,
    ): List<KontaktinfoTiltaksansvarlige> {
        return tiltaksgjennomforingAdminDto.kontaktpersoner
            .filter {
                it.navEnheter.isEmpty() || it.navEnheter.contains(
                    enhetsId,
                )
            }
            .map {
                KontaktinfoTiltaksansvarlige(
                    navn = it.navn,
                    telefonnummer = it.mobilnummer,
                    enhet = it.hovedenhet,
                    epost = it.epost,
                    _rev = null,
                    _type = null,
                    _id = null,
                    _updatedAt = null,
                    _createdAt = null,
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
