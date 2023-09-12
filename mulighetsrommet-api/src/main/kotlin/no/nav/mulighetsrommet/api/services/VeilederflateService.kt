package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.plugins.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.utils.TiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.byggInnsatsgruppeFilter
import no.nav.mulighetsrommet.api.utils.byggSokeFilter
import no.nav.mulighetsrommet.api.utils.byggTiltakstypeFilter
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

    private fun tiltaksgjennomforingQuery(id: String) = """
        *[_type == "tiltaksgjennomforing" && _id == '$id'] {
          _id,
          tiltakstype->{
            _id,
            tiltakstypeNavn,
            beskrivelse,
            nokkelinfoKomponenter,
            innsatsgruppe->,
            regelverkLenker[]->,
            faneinnhold {
              forHvemInfoboks,
              forHvem,
              detaljerOgInnholdInfoboks,
              detaljerOgInnhold,
              pameldingOgVarighetInfoboks,
              pameldingOgVarighet,
            },
            delingMedBruker,
          },
          tiltaksgjennomforingNavn,
          "tiltaksnummer": tiltaksnummer.current,
          beskrivelse,
          lokasjon,
          kontaktinfoTiltaksansvarlige[]->,
          faneinnhold {
            forHvemInfoboks,
            forHvem,
            detaljerOgInnholdInfoboks,
            detaljerOgInnhold,
            pameldingOgVarighetInfoboks,
            pameldingOgVarighet,
          },
          kontaktinfoArrangor->,
        }[0]
    """.trimIndent()

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
                tiltakstype->{
                  tiltakstypeNavn
                },
                tiltaksgjennomforingNavn,
                "tiltaksnummer": tiltaksnummer.current,
                lokasjon,
                fylke,
                enheter,
                kontaktinfoArrangor->{
                  selskapsnavn
                },
              }
        """.trimIndent()

        return when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                val gjennomforinger = result.decode<List<SanityTiltaksgjennomforing>>()
                val gjennomforingerMedDbData = supplerDataFraDB(gjennomforinger, enhetsId)
                gjennomforingerMedDbData
                    .filter { filter.lokasjoner.isEmpty() || filter.lokasjoner.contains(it.lokasjon) }
                    .filter {
                        if (it.enheter.isNullOrEmpty()) {
                            it.fylke?._ref == "enhet.fylke.$fylkeId"
                        } else {
                            it.enheter.any { enhet -> enhet._ref == "enhet.lokal.$enhetsId" }
                        }
                    }
                    .filter {
                        it.tilgjengelighetsstatus !== TiltaksgjennomforingTilgjengelighetsstatus.STENGT
                    }
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun hentTiltaksgjennomforingMedBrukerdata(
        id: String,
        fnr: String,
        accessToken: String,
    ): VeilederflateTiltaksgjennomforing {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.geografiskEnhet?.enhetsnummer
        val query = tiltaksgjennomforingQuery(id)

        return when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                val gjennomforing = result.decode<SanityTiltaksgjennomforing>()
                supplerDataFraDB(listOf(gjennomforing), enhetsId).getOrElse(0) {
                    throw NotFoundException("Fant ikke gjennomføringen med id: $id")
                }
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun hentPreviewTiltaksgjennomforing(
        id: String,
    ): VeilederflateTiltaksgjennomforing {
        val query = tiltaksgjennomforingQuery(id)

        return when (val result = sanityClient.query(query, SanityPerspective.PREVIEW_DRAFTS)) {
            is SanityResponse.Result -> {
                val gjennomforing = result.decode<SanityTiltaksgjennomforing>()
                supplerDataFraDB(listOf(gjennomforing)).getOrElse(0) {
                    throw NotFoundException("Fant ikke gjennomføringen med id: $id")
                }
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private suspend fun supplerDataFraDB(
        gjennomforingerFraSanity: List<SanityTiltaksgjennomforing>,
        enhetsId: String? = null,
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
                val apiGjennomforing = gjennomforingerFraDb[UUID.fromString(sanityData._id.replace("drafts.", ""))]

                if (apiGjennomforing != null) {
                    val kontaktpersoner = enhetsId?.let { hentKontaktpersoner(apiGjennomforing, enhetsId) }
                    val arrangor = VeilederflateArrangor(
                        selskapsnavn = apiGjennomforing.arrangor.navn,
                        organisasjonsnummer = apiGjennomforing.arrangor.organisasjonsnummer,
                        lokasjon = apiGjennomforing.lokasjonArrangor,
                        kontaktperson = apiGjennomforing.arrangor.kontaktperson?.run {
                            VeilederflateArrangor.Kontaktperson(
                                navn = navn,
                                telefon = telefon,
                                epost = epost,
                            )
                        },
                    )
                    val oppstart = apiGjennomforing.oppstart.name.lowercase()
                    val oppstartsdato = apiGjennomforing.startDato
                    val sluttdato = apiGjennomforing.sluttDato
                    val fylke = apiGjennomforing.navRegion?.let { FylkeRef(_ref = "enhet.fylke.${it.enhetsnummer}") }
                    val enheter =
                        apiGjennomforing.navEnheter.map { EnhetRef(_ref = "enhet.lokal.${it.enhetsnummer}") }

                    return@map sanityData.run {
                        VeilederflateTiltaksgjennomforing(
                            _id = _id,
                            tiltakstype = tiltakstype?.run {
                                VeilederflateTiltakstype(
                                    _id = _id,
                                    tiltakstypeNavn = tiltakstypeNavn,
                                    beskrivelse = beskrivelse,
                                    nokkelinfoKomponenter = nokkelinfoKomponenter,
                                    innsatsgruppe = innsatsgruppe,
                                    regelverkLenker = regelverkLenker,
                                    faneinnhold = faneinnhold,
                                    delingMedBruker = delingMedBruker,
                                    arenakode = apiGjennomforing.tiltakstype.arenaKode,
                                )
                            },
                            tiltaksgjennomforingNavn = tiltaksgjennomforingNavn,
                            tiltaksnummer = apiGjennomforing.tiltaksnummer,
                            stengtFra = apiGjennomforing.stengtFra,
                            stengtTil = apiGjennomforing.stengtTil,
                            kontaktinfoTiltaksansvarlige = kontaktpersoner?.ifEmpty { sanityData.kontaktinfoTiltaksansvarlige },
                            oppstart = oppstart,
                            oppstartsdato = oppstartsdato,
                            sluttdato = sluttdato,
                            tilgjengelighetsstatus = apiGjennomforing.tilgjengelighet,
                            estimert_ventetid = apiGjennomforing.estimertVentetid,
                            kontaktinfoArrangor = arrangor,
                            lokasjon = apiGjennomforing.lokasjonArrangor ?: sanityData.lokasjon,
                            fylke = fylke ?: sanityData.fylke,
                            enheter = enheter.ifEmpty { sanityData.enheter },
                            beskrivelse = beskrivelse,
                            faneinnhold = faneinnhold,
                        )
                    }
                }

                val arenaKode = sanityData.tiltakstype?._id
                    ?.let { tiltakstypeService.getBySanityId(UUID.fromString(it)) }
                    ?.arenaKode

                sanityData.run {
                    VeilederflateTiltaksgjennomforing(
                        _id = _id,
                        tiltakstype = tiltakstype?.run {
                            VeilederflateTiltakstype(
                                _id = _id,
                                tiltakstypeNavn = tiltakstypeNavn,
                                beskrivelse = beskrivelse,
                                nokkelinfoKomponenter = nokkelinfoKomponenter,
                                innsatsgruppe = innsatsgruppe,
                                regelverkLenker = regelverkLenker,
                                faneinnhold = faneinnhold,
                                delingMedBruker = delingMedBruker,
                                arenakode = arenaKode,
                            )
                        },
                        tiltaksgjennomforingNavn = tiltaksgjennomforingNavn,
                        lokasjon = lokasjon,
                        fylke = fylke,
                        enheter = enheter,
                        kontaktinfoTiltaksansvarlige = kontaktinfoTiltaksansvarlige,
                        faneinnhold = faneinnhold,
                    )
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

    // TODO er vel ikke noe vits å gå mot Sanity her?
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
