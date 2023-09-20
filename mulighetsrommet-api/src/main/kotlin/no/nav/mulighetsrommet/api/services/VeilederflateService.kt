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
    private val sanityCache: Cache<String, SanityResponse.Result> = Caffeine.newBuilder()
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

    suspend fun hentInnsatsgrupper(): List<VeilederflateInnsatsgruppe> {
        val result = CacheUtils.tryCacheFirstNotNull(sanityCache, "innsatsgrupper") {
            val result = sanityClient.query(
                """
                *[_type == "innsatsgruppe"] | order(order asc)
                """.trimIndent(),
            )
            when (result) {
                is SanityResponse.Result -> result
                is SanityResponse.Error -> throw Exception(result.error.toString())
            }
        }

        return result.decode<List<SanityInnsatsgruppe>>()
            .map {
                VeilederflateInnsatsgruppe(
                    sanityId = it._id,
                    tittel = it.tittel,
                    nokkel = it.nokkel,
                    beskrivelse = it.beskrivelse,
                    order = it.order,
                )
            }
    }

    suspend fun hentTiltakstyper(): List<VeilederflateTiltakstype> {
        val result = CacheUtils.tryCacheFirstNotNull(sanityCache, "tiltakstyper") {
            val result = sanityClient.query(
                """
                    *[_type == "tiltakstype"] {
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
                    }
                """.trimIndent(),
            )

            when (result) {
                is SanityResponse.Result -> result
                is SanityResponse.Error -> throw Exception(result.error.toString())
            }
        }

        return result.decode<List<SanityTiltakstype>>()
            .map {
                val tiltakstype = tiltakstypeService.getBySanityId(UUID.fromString(it._id))
                VeilederflateTiltakstype(
                    sanityId = it._id,
                    navn = it.tiltakstypeNavn,
                    beskrivelse = it.beskrivelse,
                    innsatsgruppe = it.innsatsgruppe,
                    regelverkLenker = it.regelverkLenker,
                    faneinnhold = it.faneinnhold,
                    delingMedBruker = it.delingMedBruker,
                    arenakode = tiltakstype?.arenaKode,
                )
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
              "fylke": fylke->nummer.current,
              "enheter": enheter[]->nummer.current,
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
            .filter {
                if (it.enheter.isNullOrEmpty()) {
                    it.fylke == fylkeEnhetsnummer
                } else {
                    it.enheter.contains(enhetsnummer)
                }
            }
            .filter {
                it.tilgjengelighet !== TiltaksgjennomforingTilgjengelighetsstatus.STENGT
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
              kontaktpersoner[]{navKontaktperson->, "enheter": enheter[]->nummer.current},
              faneinnhold {
                forHvemInfoboks,
                forHvem,
                detaljerOgInnholdInfoboks,
                detaljerOgInnhold,
                pameldingOgVarighetInfoboks,
                pameldingOgVarighet,
              },
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
            toVeilederTiltaksgjennomforing(sanityGjennomforing, enhetsnummer)
        } else {
            toVeilederTiltaksgjennomforing(sanityGjennomforing, apiGjennomforing, enhetsnummer)
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        sanityGjennomforing: SanityTiltaksgjennomforing,
        enhetsnummer: String?,
    ): VeilederflateTiltaksgjennomforing {
        val arenaKode = sanityGjennomforing.tiltakstype?._id
            ?.let { tiltakstypeService.getBySanityId(UUID.fromString(it)) }
            ?.arenaKode

        return sanityGjennomforing.run {
            val kontaktpersoner =
                kontaktpersoner?.filter { it.enheter.contains(enhetsnummer) }?.map { it.navKontaktperson }
                    ?: kontaktinfoTiltaksansvarlige?.filter { it.enhet === enhetsnummer }
            VeilederflateTiltaksgjennomforing(
                sanityId = _id,
                tiltakstype = tiltakstype?.run {
                    VeilederflateTiltakstype(
                        sanityId = _id,
                        navn = tiltakstypeNavn,
                        beskrivelse = beskrivelse,
                        innsatsgruppe = innsatsgruppe,
                        regelverkLenker = regelverkLenker,
                        faneinnhold = faneinnhold,
                        delingMedBruker = delingMedBruker,
                        arenakode = arenaKode,
                    )
                },
                navn = tiltaksgjennomforingNavn,
                lokasjon = lokasjon,
                fylke = fylke,
                enheter = enheter,
                kontaktinfoTiltaksansvarlige = kontaktpersoner,
                faneinnhold = faneinnhold,
            )
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        sanityGjennomforing: SanityTiltaksgjennomforing,
        apiGjennomforing: TiltaksgjennomforingAdminDto,
        enhetsnummer: String?,
    ): VeilederflateTiltaksgjennomforing {
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

        val kontaktpersoner = enhetsnummer
            ?.let { utledKontaktpersonerForEnhet(apiGjennomforing, enhetsnummer) }
            ?.ifEmpty { sanityGjennomforing.kontaktinfoTiltaksansvarlige }

        val fylke = apiGjennomforing.navRegion?.enhetsnummer ?: sanityGjennomforing.fylke
        val enheter = apiGjennomforing.navEnheter
            .map { it.enhetsnummer }
            .ifEmpty { sanityGjennomforing.enheter }

        return sanityGjennomforing.run {
            VeilederflateTiltaksgjennomforing(
                sanityId = _id,
                tiltakstype = tiltakstype?.run {
                    VeilederflateTiltakstype(
                        sanityId = _id,
                        navn = tiltakstypeNavn,
                        beskrivelse = beskrivelse,
                        innsatsgruppe = innsatsgruppe,
                        regelverkLenker = regelverkLenker,
                        faneinnhold = faneinnhold,
                        delingMedBruker = delingMedBruker,
                        arenakode = apiGjennomforing.tiltakstype.arenaKode,
                    )
                },
                navn = tiltaksgjennomforingNavn,
                tiltaksnummer = apiGjennomforing.tiltaksnummer,
                stengtFra = apiGjennomforing.stengtFra,
                stengtTil = apiGjennomforing.stengtTil,
                kontaktinfoTiltaksansvarlige = kontaktpersoner,
                oppstart = apiGjennomforing.oppstart,
                oppstartsdato = apiGjennomforing.startDato,
                sluttdato = apiGjennomforing.sluttDato,
                tilgjengelighet = apiGjennomforing.tilgjengelighet,
                estimertVentetid = apiGjennomforing.estimertVentetid,
                arrangor = arrangor,
                lokasjon = apiGjennomforing.lokasjonArrangor ?: sanityGjennomforing.lokasjon,
                fylke = fylke,
                enheter = enheter,
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
