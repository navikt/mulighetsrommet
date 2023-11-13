package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.plugins.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.routes.v1.GetRelevanteTiltaksgjennomforingerForBrukerRequest
import no.nav.mulighetsrommet.api.routes.v1.GetRelevanteTiltaksgjennomforingerPreviewRequest
import no.nav.mulighetsrommet.api.routes.v1.GetTiltaksgjennomforingForBrukerRequest
import no.nav.mulighetsrommet.api.utils.byggInnsatsgruppeFilter
import no.nav.mulighetsrommet.api.utils.byggSokeFilter
import no.nav.mulighetsrommet.api.utils.byggTiltakstypeFilter
import no.nav.mulighetsrommet.api.utils.utledInnsatsgrupper
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class VeilederflateService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
    private val tiltakstypeService: TiltakstypeService,
    private val navEnhetService: NavEnhetService,
) {
    private val sanityCache: Cache<String, SanityResponse.Result> = Caffeine.newBuilder()
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

    suspend fun hentPreviewTiltaksgjennomforinger(
        filter: GetRelevanteTiltaksgjennomforingerPreviewRequest,
    ): List<VeilederflateTiltaksgjennomforing> {
        return hentTiltaksgjennomforinger(
            filter.innsatsgruppe,
            filter.tiltakstypeIds,
            filter.search,
            listOf(filter.geografiskEnhet),
        )
    }

    suspend fun hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
        filter: GetRelevanteTiltaksgjennomforingerForBrukerRequest,
        brukersEnheter: List<String>,
    ): List<VeilederflateTiltaksgjennomforing> {
        if (brukersEnheter.isEmpty()) {
            return emptyList()
        }

        return hentTiltaksgjennomforinger(
            filter.innsatsgruppe,
            filter.tiltakstypeIds,
            filter.search,
            brukersEnheter,
        )
    }

    private suspend fun hentTiltaksgjennomforinger(
        innsatsgruppe: String?,
        tiltakstypeIds: List<String>?,
        search: String?,
        brukersEnheter: List<String>,
    ): List<VeilederflateTiltaksgjennomforing> {
        val query = """
            *[_type == "tiltaksgjennomforing"
              ${byggInnsatsgruppeFilter(innsatsgruppe)}
              ${byggTiltakstypeFilter(tiltakstypeIds)}
              ${byggSokeFilter(search)}
            ] {
              _id,
              tiltakstype->{
                _id,
                tiltakstypeNavn
              },
              tiltaksgjennomforingNavn,
              "tiltaksnummer": tiltaksnummer.current,
              stedForGjennomforing,
              "fylke": fylke->nummer.current,
              "enheter": coalesce(enheter[]->nummer.current, []),
            }
        """.trimIndent()

        val sanityGjennomforinger = when (val result = sanityClient.query(query)) {
            is SanityResponse.Result -> result.decode<List<SanityTiltaksgjennomforing>>()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }

        val fylkeEnhetsnummer = brukersEnheter.firstOrNull()
            ?.let { navEnhetService.hentOverordnetFylkesenhet(it)?.enhetsnummer }
            ?: ""

        val gruppeGjennomforinger = tiltaksgjennomforingService.getAllVeilederflateTiltaksgjennomforing(
            search = search,
            sanityTiltakstypeIds = tiltakstypeIds?.map { UUID.fromString(it) },
            innsatsgrupper = innsatsgruppe?.let {
                utledInnsatsgrupper(innsatsgruppe).map { Innsatsgruppe.valueOf(it) }
            } ?: emptyList(),
            brukersEnheter = brukersEnheter,
        )

        val gruppeSanityIds = gruppeGjennomforinger.map { it.sanityId }

        val individuelleGjennomforinger = sanityGjennomforinger
            .filter { it._id !in gruppeSanityIds }
            .map { toVeilederTiltaksgjennomforing(it, brukersEnheter) }

        return (individuelleGjennomforinger + gruppeGjennomforinger)
            .filter {
                if (it.enheter.isNullOrEmpty()) {
                    it.fylke == fylkeEnhetsnummer // TODO Trengs denne lenger? Kan vi ta den bort?
                } else {
                    it.enheter.any { enhet -> enhet in brukersEnheter }
                }
            }
            .filter {
                it.tilgjengelighet !== TiltaksgjennomforingTilgjengelighetsstatus.STENGT
            }
    }

    suspend fun hentTiltaksgjennomforingMedBrukerdata(
        request: GetTiltaksgjennomforingForBrukerRequest,
        brukersEnheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val apiGjennomforing = tiltaksgjennomforingService.get(request.id)

        if (apiGjennomforing != null) {
            val tiltakstype = tiltakstypeService.getById(apiGjennomforing.tiltakstype.id)
            val sanityTiltakstype = hentTiltakstyper().find { it.sanityId == tiltakstype?.sanityId.toString() }
                ?: throw NotFoundException("Fant ikke tiltakstype for gjennomføring med id: '${request.id}'")
            return toVeilederTiltaksgjennomforing(apiGjennomforing, sanityTiltakstype, brukersEnheter)
        }

        val sanityTiltaksgjennomforing =
            getSanityTiltaksgjennomforing(request.id.toString(), SanityPerspective.PUBLISHED)
        return toVeilederTiltaksgjennomforing(sanityTiltaksgjennomforing, brukersEnheter)
    }

    suspend fun hentPreviewTiltaksgjennomforing(
        id: String,
        brukersEnheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val sanitizedSanityId = UUID.fromString(id.replace("drafts.", ""))
        val apiGjennomforing = tiltaksgjennomforingService.get(sanitizedSanityId)

        if (apiGjennomforing != null) {
            val tiltakstype = tiltakstypeService.getById(apiGjennomforing.tiltakstype.id)
            val sanityTiltakstype = hentTiltakstyper()
                .find { it.sanityId == tiltakstype?.sanityId.toString() }
                ?: throw NotFoundException("Fant ikke tiltakstype for gjennomføring med id: '$sanitizedSanityId'")
            return toVeilederTiltaksgjennomforing(apiGjennomforing, sanityTiltakstype, brukersEnheter)
        }

        val sanityGjennomforing = getSanityTiltaksgjennomforing(id, SanityPerspective.PREVIEW_DRAFTS)
        return toVeilederTiltaksgjennomforing(sanityGjennomforing, brukersEnheter)
    }

    private suspend fun getSanityTiltaksgjennomforing(
        id: String,
        perspective: SanityPerspective,
    ): SanityTiltaksgjennomforing {
        val query = """
            *[_type == "tiltaksgjennomforing" && _id == "$id"] {
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
              stedForGjennomforing,
              kontaktpersoner[]{navKontaktperson->, "enheter": coalesce(enheter[]->nummer.current, [])},
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
            is SanityResponse.Result -> result.decode()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        sanityGjennomforing: SanityTiltaksgjennomforing,
        brukersEnheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val arenaKode = tiltakstypeService.getBySanityId(UUID.fromString(sanityGjennomforing.tiltakstype._id))
            ?.arenaKode

        return sanityGjennomforing.run {
            val kontaktpersoner = kontaktpersoner
                ?.filter { it.enheter.any { enhet -> enhet in brukersEnheter } }
                ?.map { it.navKontaktperson }
                ?: emptyList()

            VeilederflateTiltaksgjennomforing(
                sanityId = _id,
                tiltakstype = tiltakstype.run {
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
                stedForGjennomforing = stedForGjennomforing,
                fylke = fylke,
                enheter = enheter,
                kontaktinfoTiltaksansvarlige = kontaktpersoner,
                faneinnhold = faneinnhold,
                beskrivelse = beskrivelse,
                oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
            )
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        apiGjennomforing: TiltaksgjennomforingAdminDto,
        veilederflateTiltakstype: VeilederflateTiltakstype,
        brukersEnheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val arrangor = VeilederflateArrangor(
            selskapsnavn = apiGjennomforing.arrangor.navn,
            organisasjonsnummer = apiGjennomforing.arrangor.organisasjonsnummer,
            kontaktperson = apiGjennomforing.arrangor.kontaktperson?.run {
                VeilederflateArrangor.Kontaktperson(
                    navn = navn,
                    telefon = telefon,
                    epost = epost,
                )
            },
        )

        val kontaktpersoner = utledKontaktpersonerForEnhet(apiGjennomforing, brukersEnheter)

        val fylke = apiGjennomforing.navRegion?.enhetsnummer
        val enheter = apiGjennomforing.navEnheter.map { it.enhetsnummer }

        return apiGjennomforing.run {
            VeilederflateTiltaksgjennomforing(
                id = id,
                tiltakstype = veilederflateTiltakstype,
                navn = navn,
                tiltaksnummer = apiGjennomforing.tiltaksnummer,
                stengtFra = apiGjennomforing.stengtFra,
                stengtTil = apiGjennomforing.stengtTil,
                kontaktinfoTiltaksansvarlige = kontaktpersoner,
                oppstart = apiGjennomforing.oppstart,
                oppstartsdato = apiGjennomforing.startDato,
                sluttdato = apiGjennomforing.sluttDato,
                tilgjengelighet = apiGjennomforing.tilgjengelighet,
                arrangor = arrangor,
                stedForGjennomforing = apiGjennomforing.stedForGjennomforing,
                fylke = fylke,
                enheter = enheter,
                beskrivelse = apiGjennomforing.beskrivelse ?: beskrivelse,
                faneinnhold = faneinnhold,
            )
        }
    }

    private fun utledKontaktpersonerForEnhet(
        tiltaksgjennomforingAdminDto: TiltaksgjennomforingAdminDto,
        brukersEnheter: List<String>,
    ): List<KontaktinfoTiltaksansvarlige> {
        return tiltaksgjennomforingAdminDto.kontaktpersoner
            .filter { it.navEnheter.isEmpty() || it.navEnheter.any { enhet -> enhet in brukersEnheter } }
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
