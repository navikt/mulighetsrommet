package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.utils.TiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.byggInnsatsgruppeFilter
import no.nav.mulighetsrommet.api.utils.byggSokeFilter
import no.nav.mulighetsrommet.api.utils.byggTiltakstypeFilter
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class VeilederflateService(
    private val sanityClient: SanityClient,
    private val brukerService: BrukerService,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
    private val tiltakstypeService: TiltakstypeService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fylkenummerCache = mutableMapOf<String?, String>()
    private val sanityCache: Cache<String, SanityResponse> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    private val lokasjonCache: Cache<String, List<String>> = Caffeine.newBuilder()
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
                *[_type == "innsatsgruppe"] | order(order asc)
                """.trimIndent(),
            )
        }
    }

    suspend fun hentTiltakstyper(): SanityResponse {
        return CacheUtils.tryCacheFirstNotNull(sanityCache, "tiltakstyper") {
            sanityClient.query(
                """
                    *[_type == "tiltakstype"]
                """.trimIndent(),
            )
        }
    }

    suspend fun hentLokasjonerForBrukersEnhetOgFylke(fnr: String, accessToken: String): List<String> {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.geografiskEnhet?.enhetsnummer ?: ""
        val fylkeId = getFylkeIdBasertPaaEnhetsId(enhetsId) ?: ""

        return CacheUtils.tryCacheFirstNotNull(lokasjonCache, fnr) {
            tiltaksgjennomforingService.getLokasjonerForBrukersEnhet(enhetsId, fylkeId)
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
            *[_type == "tiltaksgjennomforing"
              ${byggInnsatsgruppeFilter(filter.innsatsgruppe)}
              ${byggTiltakstypeFilter(filter.tiltakstypeIder)}
              ${byggSokeFilter(filter.sokestreng)}
              ]
              {
                _id,
                tiltaksgjennomforingNavn,
                oppstart,
                lokasjon,
                oppstartsdato,
                "tiltaksnummer": tiltaksnummer.current,
                kontaktinfoArrangor->{selskapsnavn},
                tiltakstype->{tiltakstypeNavn},
                fylke,
                enheter
              }
        """.trimIndent()

        return when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                val gjennomforinger = result.decode<List<VeilederflateTiltaksgjennomforing>>()
                val gjennomforingerMedDbData = supplerDataFraDB(gjennomforinger, enhetsId)
                gjennomforingerMedDbData
                    .filter { filter.lokasjoner.isEmpty() || filter.lokasjoner.contains(it.lokasjon) }
                    .filter {
                        if (it.enheter.isNullOrEmpty()) {
                            it.fylke?._ref == "enhet.fylke.$fylkeId"
                        } else {
                            it.enheter.any { it._ref == "enhet.lokal.$enhetsId" }
                        }
                    }
                    .filter {
                        it.tilgjengelighetsstatus !== TiltaksgjennomforingTilgjengelighetsstatus.STENGT
                    }
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun hentTiltaksgjennomforing(
        id: String,
        fnr: String?,
        accessToken: String,
    ): List<VeilederflateTiltaksgjennomforing> {
        val brukerData = fnr?.let { brukerService.hentBrukerdata(fnr, accessToken) }
        val enhetsId = brukerData?.geografiskEnhet?.enhetsnummer ?: ""
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

        return when (val result = sanityClient.query(query, SanityPerspective.RAW)) {
            is SanityResponse.Result -> {
                val gjennomforinger = result.decode<List<VeilederflateTiltaksgjennomforing>>()
                supplerDataFraDB(gjennomforinger, enhetsId)
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private suspend fun supplerDataFraDB(
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
                val apiGjennomforing = gjennomforingerFraDb[UUID.fromString(sanityData._id)]

                if (apiGjennomforing != null) {
                    val kontaktpersoner = hentKontaktpersoner(apiGjennomforing, enhetsId)
                    val kontaktpersonerArrangor = apiGjennomforing.arrangor.kontaktperson?.let {
                        KontaktInfoArrangor(
                            selskapsnavn = virksomhetService.getOrSyncVirksomhet(it.organisasjonsnummer)?.navn,
                            telefonnummer = it.telefon,
                            adresse = apiGjennomforing.lokasjonArrangor,
                            epost = it.epost,
                        )
                    }
                    val oppstart = apiGjennomforing.oppstart.name.lowercase()
                    val oppstartsdato = apiGjennomforing.startDato
                    val sluttdato = apiGjennomforing.sluttDato ?: sanityData.sluttdato
                    val fylke = apiGjennomforing.navRegion?.let { FylkeRef(_ref = "enhet.fylke.${it.enhetsnummer}") }
                    val enheter =
                        apiGjennomforing.navEnheter.map { EnhetRef(_ref = "enhet.lokal.${it.enhetsnummer}") }

                    return@map sanityData.copy(
                        stengtFra = apiGjennomforing.stengtFra,
                        stengtTil = apiGjennomforing.stengtTil,
                        kontaktinfoTiltaksansvarlige = kontaktpersoner.ifEmpty { sanityData.kontaktinfoTiltaksansvarlige },
                        oppstart = oppstart,
                        oppstartsdato = oppstartsdato,
                        sluttdato = sluttdato,
                        tilgjengelighetsstatus = apiGjennomforing.tilgjengelighet,
                        estimert_ventetid = apiGjennomforing.estimertVentetid,
                        tiltakstype = sanityData.tiltakstype?.copy(arenakode = apiGjennomforing.tiltakstype.arenaKode),
                        lokasjon = apiGjennomforing.lokasjonArrangor ?: sanityData.lokasjon,
                        kontaktinfoArrangor = kontaktpersonerArrangor ?: sanityData.kontaktinfoArrangor,
                        fylke = fylke ?: sanityData.fylke,
                        enheter = enheter.ifEmpty { sanityData.enheter },
                    )
                }

                val tiltakstypeFraDb = sanityData.tiltakstype?._id?.let {
                    tiltakstypeService.getBySanityId(UUID.fromString(sanityData.tiltakstype._id))
                }
                if (tiltakstypeFraDb != null && !Tiltakskoder.isGruppetiltak(tiltakstypeFraDb.arenaKode)) {
                    sanityData.copy(
                        tiltakstype = sanityData.tiltakstype.copy(arenakode = tiltakstypeFraDb.arenaKode),
                    )
                } else {
                    sanityData
                }
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
