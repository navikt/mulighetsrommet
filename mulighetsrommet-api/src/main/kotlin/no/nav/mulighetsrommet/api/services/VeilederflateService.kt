package no.nav.mulighetsrommet.api.services

import arrow.core.NonEmptyList
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.plugins.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.routes.v1.ApentForInnsok
import no.nav.mulighetsrommet.api.utils.byggInnsatsgruppeFilter
import no.nav.mulighetsrommet.api.utils.byggSokeFilter
import no.nav.mulighetsrommet.api.utils.byggTiltakstypeFilter
import no.nav.mulighetsrommet.api.utils.utledInnsatsgrupper
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
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
                      "oppskrifter":  coalesce(oppskrifter[] -> {
                        ...,
                        steg[] {
                          ...,
                          innhold[] {
                            ...,
                            _type == "image" => {
                            ...,
                            asset-> // For å hente ut url til bilder
                        }
                      }
                    }
                    }, [])
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

    suspend fun hentTiltaksgjennomforinger(
        enheter: NonEmptyList<String>,
        innsatsgruppe: String? = null,
        tiltakstypeIds: List<String>? = null,
        search: String? = null,
        apentForInnsok: ApentForInnsok = ApentForInnsok.APENT_ELLER_STENGT,
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

        val gruppeGjennomforinger = tiltaksgjennomforingService.getAllVeilederflateTiltaksgjennomforing(
            search = search,
            sanityTiltakstypeIds = tiltakstypeIds?.map { UUID.fromString(it) },
            innsatsgrupper = innsatsgruppe
                ?.let { utledInnsatsgrupper(innsatsgruppe).map { Innsatsgruppe.valueOf(it) } }
                ?: emptyList(),
            enheter = enheter,
            apentForInnsok = when (apentForInnsok) {
                ApentForInnsok.APENT -> true
                ApentForInnsok.STENGT -> false
                ApentForInnsok.APENT_ELLER_STENGT -> null
            },
        )

        val individuelleGjennomforinger = if (apentForInnsok == ApentForInnsok.STENGT) {
            // Det er foreløpig ikke noe egen funksjonalitet for å markere tiltak som midlertidig stengt i Sanity
            emptyList()
        } else {
            sanityGjennomforinger.map { toVeilederTiltaksgjennomforing(it, enheter) }
        }

        val fylker = enheter.map {
            navEnhetService.hentOverordnetFylkesenhet(it)?.enhetsnummer
        }

        return (individuelleGjennomforinger + gruppeGjennomforinger)
            .filter {
                if (it.enheter.isNullOrEmpty()) {
                    it.fylke in fylker
                } else {
                    it.enheter.any { enhet -> enhet in enheter }
                }
            }
    }

    suspend fun hentTiltaksgjennomforing(
        id: UUID,
        enheter: List<String>,
        sanityPerspective: SanityPerspective,
    ): VeilederflateTiltaksgjennomforing {
        return tiltaksgjennomforingService.get(id)
            ?.let { gjennomforing ->
                val tiltakstype = tiltakstypeService.getById(gjennomforing.tiltakstype.id)
                val sanityTiltakstype = hentTiltakstyper()
                    .find { it.sanityId == tiltakstype?.sanityId.toString() }
                    ?: throw NotFoundException("Fant ikke tiltakstype for gjennomføring med id: '$id'")
                toVeilederTiltaksgjennomforing(gjennomforing, sanityTiltakstype, enheter)
            }
            ?: run {
                val gjennomforing = getSanityTiltaksgjennomforing(id.toString(), sanityPerspective)
                toVeilederTiltaksgjennomforing(gjennomforing, enheter)
            }
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
                "oppskrifter":  coalesce(oppskrifter[] -> {
                        ...,
                        steg[] {
                          ...,
                          innhold[] {
                            ...,
                            _type == "image" => {
                            ...,
                            asset-> // For å hente ut url til bilder
                        }
                      }
                    }
                  }, [])
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
                kontaktinfoInfoboks,
                kontaktinfo,
              },
              delingMedBruker,
            }[0]
        """.trimIndent()

        return when (val result = sanityClient.query(query, perspective)) {
            is SanityResponse.Result -> result.decode()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        sanityGjennomforing: SanityTiltaksgjennomforing,
        enheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val arenaKode = tiltakstypeService.getBySanityId(UUID.fromString(sanityGjennomforing.tiltakstype._id))
            ?.arenaKode

        return sanityGjennomforing.run {
            val kontaktpersoner = kontaktpersoner
                ?.filter { it.enheter.any { enhet -> enhet in enheter } }
                ?.map { it.navKontaktperson }
                ?.map {
                    VeilederflateKontaktinfoTiltaksansvarlig(
                        navn = it.navn,
                        telefonnummer = it.telefonnummer,
                        enhet = it.enhet?.let { enhet -> navEnhetService.hentEnhet(enhet) },
                        epost = it.epost,
                        beskrivelse = it.beskrivelse,
                    )
                }
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
                navn = tiltaksgjennomforingNavn ?: "",
                apentForInnsok = true,
                stedForGjennomforing = stedForGjennomforing,
                fylke = fylke,
                enheter = this.enheter?.filterNotNull(),
                kontaktinfoTiltaksansvarlige = kontaktpersoner,
                faneinnhold = faneinnhold?.copy(delMedBruker = delingMedBruker),
                beskrivelse = beskrivelse,
                oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                tiltaksnummer = tiltaksnummer ?: "",
                kontaktinfo = VeilederflateKontaktinfo(
                    varsler = emptyList(),
                    tiltaksansvarlige = kontaktpersoner,
                ),
            )
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        apiGjennomforing: TiltaksgjennomforingAdminDto,
        veilederflateTiltakstype: VeilederflateTiltakstype,
        enheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val arrangor = VeilederflateArrangor(
            selskapsnavn = apiGjennomforing.arrangor.navn,
            organisasjonsnummer = apiGjennomforing.arrangor.organisasjonsnummer,
            kontaktpersoner = apiGjennomforing.arrangor.kontaktpersoner.map {
                VeilederflateArrangor.Kontaktperson(
                    navn = it.navn,
                    telefon = it.telefon,
                    epost = it.epost,
                )
            },
        )

        val kontaktpersoner = utledKontaktpersonerForEnhet(apiGjennomforing, enheter)

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
                apentForInnsok = apiGjennomforing.apentForInnsok,
                arrangor = arrangor,
                stedForGjennomforing = apiGjennomforing.stedForGjennomforing,
                fylke = apiGjennomforing.navRegion?.enhetsnummer,
                enheter = apiGjennomforing.navEnheter.map { it.enhetsnummer },
                beskrivelse = apiGjennomforing.beskrivelse ?: beskrivelse,
                faneinnhold = faneinnhold,
                kontaktinfo = VeilederflateKontaktinfo(
                    varsler = emptyList(),
                    tiltaksansvarlige = kontaktpersoner,
                ),
            )
        }
    }

    private fun utledKontaktpersonerForEnhet(
        tiltaksgjennomforingAdminDto: TiltaksgjennomforingAdminDto,
        enheter: List<String>,
    ): List<VeilederflateKontaktinfoTiltaksansvarlig> {
        return tiltaksgjennomforingAdminDto.kontaktpersoner
            .filter { it.navEnheter.isEmpty() || it.navEnheter.any { enhet -> enhet in enheter } }
            .map {
                VeilederflateKontaktinfoTiltaksansvarlig(
                    navn = it.navn,
                    telefonnummer = it.mobilnummer,
                    enhet = navEnhetService.hentEnhet(it.hovedenhet),
                    epost = it.epost,
                    beskrivelse = it.beskrivelse,
                )
            }
    }

    suspend fun hentOppskrifterForTiltakstype(tiltakstypeId: String, perspective: SanityPerspective): List<Oppskrift> {
        val query = """
              *[_type == "tiltakstype" && defined(oppskrifter) && _id == '$tiltakstypeId'] {
               oppskrifter[] -> {
                  ...,
                  steg[] {
                    ...,
                    innhold[] {
                      ...,
                      _type == "image" => {
                      ...,
                      asset-> // For å hente ut url til bilder
                      }
                    }
                  }
               }
             }.oppskrifter[]
        """.trimIndent()

        return when (val result = sanityClient.query(query, perspective)) {
            is SanityResponse.Result -> result.decode()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }
}
