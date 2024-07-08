package no.nav.mulighetsrommet.api.services

import arrow.core.NonEmptyList
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.plugins.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityParam
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.routes.v1.ApentForInnsok
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatusDto
import no.nav.mulighetsrommet.metrics.Metrikker
import java.util.*
import java.util.concurrent.TimeUnit

class VeilederflateService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
    private val tiltakstypeService: TiltakstypeService,
    private val navEnhetService: NavEnhetService,
) {
    private val sanityTiltakstyperCache: Cache<String, List<SanityTiltakstype>> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    private val sanityTiltaksgjennomforingerCache: Cache<String, List<SanityTiltaksgjennomforing>> = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("sanityTiltakstyperCache", sanityTiltakstyperCache)
        cacheMetrics.addCache("sanityTiltaksgjennomforingerCache", sanityTiltaksgjennomforingerCache)
    }

    fun hentInnsatsgrupper(): List<VeilederflateInnsatsgruppe> {
        // TODO: benytt verdi for GRADERT_VARIG_TILPASSET_INNSATS når ny 14a-løsning er lansert nasjonalt
        return (Innsatsgruppe.entries - Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS)
            .map {
                VeilederflateInnsatsgruppe(
                    tittel = it.tittel,
                    nokkel = it.name,
                    order = it.order,
                )
            }
    }

    suspend fun hentTiltakstyper(): List<VeilederflateTiltakstype> {
        return getSanityTiltakstyper()
            .map {
                val tiltakstype = tiltakstypeService.getBySanityId(UUID.fromString(it._id))
                VeilederflateTiltakstype(
                    sanityId = it._id,
                    navn = it.tiltakstypeNavn,
                    beskrivelse = it.beskrivelse,
                    innsatsgrupper = it.innsatsgrupper,
                    regelverkLenker = it.regelverkLenker,
                    faneinnhold = it.faneinnhold,
                    delingMedBruker = it.delingMedBruker,
                    arenakode = tiltakstype?.arenaKode,
                    tiltakskode = tiltakstype?.tiltakskode,
                    kanKombineresMed = it.kanKombineresMed,
                )
            }
    }

    private suspend fun getSanityTiltakstyper(): List<SanityTiltakstype> {
        sanityTiltakstyperCache.getIfPresent("tiltakstyper")?.let { return@getSanityTiltakstyper it }

        val result = sanityClient.query(
            """
                    *[_type == "tiltakstype"] {
                      _id,
                      tiltakstypeNavn,
                      beskrivelse,
                      nokkelinfoKomponenter,
                      innsatsgrupper,
                      "kanKombineresMed": coalesce(kombinasjon[]->{tiltakstypeNavn}.tiltakstypeNavn, []),
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

        return when (result) {
            is SanityResponse.Result -> result.decode<List<SanityTiltakstype>>()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
            .also {
                sanityTiltakstyperCache.put("tiltakstyper", it)
            }
    }

    suspend fun hentTiltaksgjennomforinger(
        enheter: NonEmptyList<String>,
        tiltakstypeIds: List<String>? = null,
        innsatsgruppe: Innsatsgruppe,
        apentForInnsok: ApentForInnsok = ApentForInnsok.APENT_ELLER_STENGT,
        search: String? = null,
        cacheUsage: CacheUsage,
    ): List<VeilederflateTiltaksgjennomforing> = coroutineScope {
        val individuelleGjennomforinger = async {
            hentSanityTiltak(enheter, tiltakstypeIds, innsatsgruppe, apentForInnsok, search, cacheUsage)
        }

        val gruppeGjennomforinger = async {
            hentGruppetiltak(enheter, tiltakstypeIds, innsatsgruppe, apentForInnsok, search)
        }

        (individuelleGjennomforinger.await() + gruppeGjennomforinger.await())
    }

    private suspend fun hentSanityTiltak(
        enheter: NonEmptyList<String>,
        tiltakstypeIds: List<String>?,
        innsatsgruppe: Innsatsgruppe,
        apentForInnsok: ApentForInnsok,
        search: String?,
        cacheUsage: CacheUsage,
    ): List<VeilederflateTiltaksgjennomforing> {
        if (apentForInnsok == ApentForInnsok.STENGT) {
            // Det er foreløpig ikke noe egen funksjonalitet for å markere tiltak som midlertidig stengt i Sanity
            return emptyList()
        }

        val sanityGjennomforinger = sgetSanityTiltak(search, cacheUsage)

        val fylker = enheter.map {
            navEnhetService.hentOverordnetFylkesenhet(it)?.enhetsnummer
        }

        return sanityGjennomforinger
            .filter { tiltakstypeIds.isNullOrEmpty() || tiltakstypeIds.contains(it.tiltakstype._id) }
            .filter { it.tiltakstype.innsatsgrupper != null && it.tiltakstype.innsatsgrupper.contains(innsatsgruppe) }
            .map { toVeilederTiltaksgjennomforing(it, enheter) }
            .filter {
                if (it.enheter.isNullOrEmpty()) {
                    it.fylke in fylker
                } else {
                    it.enheter.any { enhet -> enhet in enheter }
                }
            }
    }

    private suspend fun sgetSanityTiltak(search: String?, cacheUsage: CacheUsage): List<SanityTiltaksgjennomforing> {
        sanityTiltaksgjennomforingerCache.getIfPresent(search ?: "")?.let {
            if (cacheUsage == CacheUsage.UseCache) {
                return@sgetSanityTiltak it
            }
        }

        val query = """
            *[_type == "tiltaksgjennomforing"
              ${if (search != null) "&& [tiltaksgjennomforingNavn, string(tiltaksnummer.current), tiltakstype->tiltakstypeNavn] match \$search" else ""}
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

        val params = buildList {
            if (search != null) {
                add(SanityParam.of("search", "*$search*"))
            }
        }

        return when (val result = sanityClient.query(query, params)) {
            is SanityResponse.Result -> result.decode<List<SanityTiltaksgjennomforing>>()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
            .also {
                sanityTiltaksgjennomforingerCache.put(search ?: "", it)
            }
    }

    private fun hentGruppetiltak(
        enheter: NonEmptyList<String>,
        tiltakstypeIds: List<String>?,
        innsatsgruppe: Innsatsgruppe,
        apentForInnsok: ApentForInnsok,
        search: String?,
    ): List<VeilederflateTiltaksgjennomforing> {
        return tiltaksgjennomforingService.getAllVeilederflateTiltaksgjennomforing(
            search = search,
            sanityTiltakstypeIds = tiltakstypeIds?.map { UUID.fromString(it) },
            innsatsgruppe = innsatsgruppe,
            enheter = enheter,
            apentForInnsok = when (apentForInnsok) {
                ApentForInnsok.APENT -> true
                ApentForInnsok.STENGT -> false
                ApentForInnsok.APENT_ELLER_STENGT -> null
            },
        )
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
                val gjennomforing = getSanityTiltaksgjennomforing(id, sanityPerspective)
                toVeilederTiltaksgjennomforing(gjennomforing, enheter)
            }
    }

    private suspend fun getSanityTiltaksgjennomforing(
        id: UUID,
        perspective: SanityPerspective,
    ): SanityTiltaksgjennomforing {
        val query = """
            *[_type == "tiltaksgjennomforing" && _id == ${'$'}id] {
              _id,
              tiltakstype->{
                _id,
                tiltakstypeNavn,
                beskrivelse,
                nokkelinfoKomponenter,
                innsatsgrupper,
                "kanKombineresMed": coalesce(kombinasjon[]->{tiltakstypeNavn}.tiltakstypeNavn, []),
                regelverkLenker[]->,
                faneinnhold {
                  forHvemInfoboks,
                  forHvem,
                  detaljerOgInnholdInfoboks,
                  detaljerOgInnhold,
                  pameldingOgVarighetInfoboks,
                  pameldingOgVarighet
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
                lenker,
                oppskrift
              },
              delingMedBruker,
            }[0]
        """.trimIndent()

        val params = listOf(SanityParam.of("id", id))

        return when (val result = sanityClient.query(query, params, perspective)) {
            is SanityResponse.Result -> result.decode()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        sanityGjennomforing: SanityTiltaksgjennomforing,
        enheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val tiltakstypeFraSanity = tiltakstypeService.getBySanityId(UUID.fromString(sanityGjennomforing.tiltakstype._id))

        return sanityGjennomforing.run {
            val kontaktpersoner = kontaktpersoner
                ?.filter { it.enheter.any { enhet -> enhet in enheter } }
                ?.mapNotNull { it.navKontaktperson }
                ?.map {
                    VeilederflateKontaktinfoTiltaksansvarlig(
                        navn = it.navn,
                        telefonnummer = it.telefonnummer,
                        enhet = it.enhet?.let { enhet -> navEnhetService.hentEnhet(enhet) },
                        epost = it.epost,
                        beskrivelse = it.beskrivelse,
                    )
                } ?: emptyList()

            VeilederflateTiltaksgjennomforing(
                avtaleId = UUID.randomUUID(),
                status = TiltaksgjennomforingStatusDto(
                    status = TiltaksgjennomforingStatus.GJENNOMFORES,
                    avbrutt = null,
                ),
                sanityId = _id,
                tiltakstype = tiltakstype.run {
                    VeilederflateTiltakstype(
                        sanityId = _id,
                        navn = tiltakstypeNavn,
                        beskrivelse = beskrivelse,
                        innsatsgrupper = innsatsgrupper,
                        regelverkLenker = regelverkLenker,
                        faneinnhold = faneinnhold,
                        delingMedBruker = delingMedBruker,
                        arenakode = tiltakstypeFraSanity?.arenaKode,
                        tiltakskode = tiltakstypeFraSanity?.tiltakskode,
                        kanKombineresMed = kanKombineresMed,
                    )
                },
                navn = tiltaksgjennomforingNavn ?: "",
                apentForInnsok = true,
                stedForGjennomforing = stedForGjennomforing,
                fylke = fylke,
                enheter = this.enheter?.filterNotNull(),
                faneinnhold = faneinnhold?.copy(delMedBruker = delingMedBruker),
                beskrivelse = beskrivelse,
                oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                tiltaksnummer = tiltaksnummer ?: "",
                kontaktinfo = VeilederflateKontaktinfo(
                    varsler = emptyList(),
                    tiltaksansvarlige = kontaktpersoner,
                ),
                personvernBekreftet = false, // Individuelle tiltak har ikke informasjon om personvern
            )
        }
    }

    private fun toVeilederTiltaksgjennomforing(
        apiGjennomforing: TiltaksgjennomforingAdminDto,
        veilederflateTiltakstype: VeilederflateTiltakstype,
        enheter: List<String>,
    ): VeilederflateTiltaksgjennomforing {
        val arrangor = VeilederflateArrangor(
            arrangorId = apiGjennomforing.arrangor.id,
            selskapsnavn = apiGjennomforing.arrangor.navn,
            organisasjonsnummer = apiGjennomforing.arrangor.organisasjonsnummer,
            kontaktpersoner = apiGjennomforing.arrangor.kontaktpersoner.map {
                VeilederflateArrangorKontaktperson(
                    id = it.id,
                    navn = it.navn,
                    epost = it.epost,
                    telefon = it.telefon,
                    beskrivelse = it.beskrivelse,
                )
            },
        )

        val kontaktpersoner = utledKontaktpersonerForEnhet(apiGjennomforing, enheter)

        return apiGjennomforing.run {
            VeilederflateTiltaksgjennomforing(
                id = id,
                status = status,
                avtaleId = avtaleId,
                tiltakstype = veilederflateTiltakstype,
                navn = navn,
                tiltaksnummer = apiGjennomforing.tiltaksnummer,
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
                estimertVentetid = estimertVentetid?.let {
                    EstimertVentetid(
                        verdi = it.verdi,
                        enhet = it.enhet,
                    )
                },
                personvernBekreftet = personvernBekreftet,
            )
        }
    }

    private fun utledKontaktpersonerForEnhet(
        tiltaksgjennomforingAdminDto: TiltaksgjennomforingAdminDto,
        enheter: List<String>,
    ): List<VeilederflateKontaktinfoTiltaksansvarlig> {
        return tiltaksgjennomforingAdminDto.kontaktpersoner
            .filter { enheter.isEmpty() || it.navEnheter.isEmpty() || it.navEnheter.any { enhet -> enhet in enheter } }
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

    suspend fun hentOppskrifterForTiltakstype(
        tiltakstypeId: UUID,
        perspective: SanityPerspective,
    ): List<Oppskrift> {
        val query = """
              *[_type == "tiltakstype" && defined(oppskrifter) && _id == ${'$'}id] {
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

        val params = listOf(SanityParam.of("id", tiltakstypeId))

        return when (val result = sanityClient.query(query, params, perspective)) {
            is SanityResponse.Result -> result.decode()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }
}

enum class CacheUsage { UseCache, NoCache }
