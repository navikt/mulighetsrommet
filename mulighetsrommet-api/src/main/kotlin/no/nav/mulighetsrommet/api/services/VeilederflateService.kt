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
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class VeilederflateService(
    private val sanityClient: SanityClient,
    private val brukerService: BrukerService,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
    private val tiltakstypeService: TiltakstypeService,
    private val navEnhetService: NavEnhetService,
) {
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
        val enhetsnummer = brukerData.geografiskEnhet?.enhetsnummer
        val fylkeEnhetsnummer = enhetsnummer
            ?.let { navEnhetService.hentOverorndetFylkesenhet(it)?.enhetsnummer }
            ?: ""

        return CacheUtils.tryCacheFirstNotNull(lokasjonCache, fnr) {
            tiltaksgjennomforingService.getLokasjonerForBrukersEnhet(enhetsnummer ?: "", fylkeEnhetsnummer)
        }
    }

    suspend fun hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
        fnr: String,
        accessToken: String,
        filter: TiltaksgjennomforingFilter,
    ): List<VeilederflateTiltaksgjennomforing> {
        val query = """
            *[_type == "tiltaksgjennomforing"
              ${byggInnsatsgruppeFilter(filter.innsatsgruppe)}
              ${byggTiltakstypeFilter(filter.tiltakstypeIder)}
              ${byggSokeFilter(filter.sokestreng)}
            ] {
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

        val sanityGjennomforinger = when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> result.decode<List<SanityTiltaksgjennomforing>>()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }

        val apiGjennomforinger = sanityGjennomforinger
            .map { UUID.fromString(it._id) }
            .let { tiltaksgjennomforingService.getBySanityIds(it) }

        val brukerdata = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsnummer = brukerdata.geografiskEnhet?.enhetsnummer
        val fylkeEnhetsnummer = enhetsnummer
            ?.let { navEnhetService.hentOverorndetFylkesenhet(it)?.enhetsnummer }
            ?: ""

        return sanityGjennomforinger
            .map { sanityGjennomforing ->
                val apiGjennomforing = apiGjennomforinger[UUID.fromString(sanityGjennomforing._id)]
                mergeSanityTiltaksgjennomforingWithApiTiltaksgjennomforing(
                    sanityGjennomforing,
                    apiGjennomforing,
                    enhetsnummer,
                )
            }
            .filter { filter.lokasjoner.isEmpty() || filter.lokasjoner.contains(it.lokasjon) }
            .filter {
                if (it.enheter.isNullOrEmpty()) {
                    it.fylke?._ref == "enhet.fylke.$fylkeEnhetsnummer"
                } else {
                    it.enheter.any { enhet -> enhet._ref == "enhet.lokal.$enhetsnummer" }
                }
            }
            .filter {
                it.tilgjengelighetsstatus !== TiltaksgjennomforingTilgjengelighetsstatus.STENGT
            }
    }

    suspend fun hentTiltaksgjennomforingMedBrukerdata(
        id: String,
        fnr: String,
        accessToken: String,
    ): VeilederflateTiltaksgjennomforing {
        val sanityGjennomforing = getSanityTiltaksgjennomforing(id, SanityPerspective.PUBLISHED)
        val apiGjennomforing = tiltaksgjennomforingService.getBySanityId(UUID.fromString(id))

        val brukerdata = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsnummer = brukerdata.geografiskEnhet?.enhetsnummer

        return mergeSanityTiltaksgjennomforingWithApiTiltaksgjennomforing(
            sanityGjennomforing,
            apiGjennomforing,
            enhetsnummer,
        )
    }

    suspend fun hentPreviewTiltaksgjennomforing(id: String): VeilederflateTiltaksgjennomforing {
        val sanitizedSanityId = UUID.fromString(id.replace("drafts.", ""))
        val sanityGjennomforing = getSanityTiltaksgjennomforing(id, SanityPerspective.PREVIEW_DRAFTS)
        val apiGjennomforing = tiltaksgjennomforingService.getBySanityId(sanitizedSanityId)

        return mergeSanityTiltaksgjennomforingWithApiTiltaksgjennomforing(sanityGjennomforing, apiGjennomforing)
    }

    private suspend fun getSanityTiltaksgjennomforing(
        id: String,
        perspective: SanityPerspective,
    ): SanityTiltaksgjennomforing {
        val query = """
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

        return when (val result = sanityClient.query(query, perspective)) {
            is SanityResponse.Result -> result.decode<SanityTiltaksgjennomforing>()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private fun mergeSanityTiltaksgjennomforingWithApiTiltaksgjennomforing(
        sanityGjennomforing: SanityTiltaksgjennomforing,
        apiGjennomforing: TiltaksgjennomforingAdminDto?,
        enhetsnummer: String? = null,
    ): VeilederflateTiltaksgjennomforing {
        return if (apiGjennomforing == null) {
            toVeilederTiltaksgjennomforing(sanityGjennomforing)
        } else {
            toVeilederTiltakagjennomforing(sanityGjennomforing, apiGjennomforing, enhetsnummer)
        }
    }

    private fun toVeilederTiltaksgjennomforing(sanityGjennomforing: SanityTiltaksgjennomforing): VeilederflateTiltaksgjennomforing {
        val arenaKode = sanityGjennomforing.tiltakstype?._id
            ?.let { tiltakstypeService.getBySanityId(UUID.fromString(it)) }
            ?.arenaKode

        return sanityGjennomforing.run {
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

    private fun toVeilederTiltakagjennomforing(
        sanityGjennomforing: SanityTiltaksgjennomforing,
        apiGjennomforing: TiltaksgjennomforingAdminDto,
        enhetsnummer: String?,
    ): VeilederflateTiltaksgjennomforing {
        val kontaktpersoner = enhetsnummer?.let { utledKontaktpersonerForEnhet(apiGjennomforing, enhetsnummer) }
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
        val enheter = apiGjennomforing.navEnheter.map { EnhetRef(_ref = "enhet.lokal.${it.enhetsnummer}") }

        return sanityGjennomforing.run {
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
                kontaktinfoTiltaksansvarlige = kontaktpersoner?.ifEmpty { sanityGjennomforing.kontaktinfoTiltaksansvarlige },
                oppstart = oppstart,
                oppstartsdato = oppstartsdato,
                sluttdato = sluttdato,
                tilgjengelighetsstatus = apiGjennomforing.tilgjengelighet,
                estimert_ventetid = apiGjennomforing.estimertVentetid,
                kontaktinfoArrangor = arrangor,
                lokasjon = apiGjennomforing.lokasjonArrangor ?: sanityGjennomforing.lokasjon,
                fylke = fylke ?: sanityGjennomforing.fylke,
                enheter = enheter.ifEmpty { sanityGjennomforing.enheter },
                beskrivelse = beskrivelse,
                faneinnhold = faneinnhold,
            )
        }
    }

    private fun utledKontaktpersonerForEnhet(
        tiltaksgjennomforingAdminDto: TiltaksgjennomforingAdminDto,
        enhetsnummer: String,
    ): List<KontaktinfoTiltaksansvarlige> {
        return tiltaksgjennomforingAdminDto.kontaktpersoner
            .filter { it.navEnheter.isEmpty() || it.navEnheter.contains(enhetsnummer) }
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
}
