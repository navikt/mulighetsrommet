package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.NonEmptyList
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.sanity.CacheUsage
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.veilederflate.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.api.veilederflate.models.Oppskrift
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateArrangor
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateArrangorKontaktperson
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateInnsatsgruppe
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateKontaktinfo
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateKontaktinfoTiltaksansvarlig
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltak
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEgenRegi
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplass
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplassAnskaffet
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltaksansvarligHovedenhet
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.routes.ApentForPamelding
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskoder
import no.nav.mulighetsrommet.utils.CachedComputation
import java.time.Duration
import java.util.UUID

class VeilederflateService(
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val navEnhetService: NavEnhetService,
) {
    private val cachedTiltakstyper = CachedComputation<List<VeilederflateTiltakstype>>(
        expireAfterWrite = Duration.ofMinutes(30),
    )

    fun hentInnsatsgrupper(): List<VeilederflateInnsatsgruppe> {
        return Innsatsgruppe.entries.map {
            VeilederflateInnsatsgruppe(
                tittel = it.tittel,
                nokkel = it,
                order = it.order,
            )
        }
    }

    suspend fun hentTiltakstyper(): List<VeilederflateTiltakstype> {
        return cachedTiltakstyper.getOrCompute {
            val bySanityId = db.session { queries.tiltakstype.getAll() }
                .filter { it.sanityId != null }
                .associateBy { it.sanityId }

            sanityService.getTiltakstyper().mapNotNull { sanityTiltakstype ->
                val tiltakstype = bySanityId[UUID.fromString(sanityTiltakstype._id)] ?: return@mapNotNull null
                VeilederflateTiltakstype(
                    id = tiltakstype.id,
                    navn = tiltakstype.navn,
                    innsatsgrupper = tiltakstype.innsatsgrupper,
                    arenakode = tiltakstype.arenakode,
                    tiltakskode = tiltakstype.tiltakskode,
                    tiltaksgruppe = tiltakstype.tiltakskode?.gruppe?.tittel,
                    sanityId = sanityTiltakstype._id,
                    beskrivelse = sanityTiltakstype.beskrivelse,
                    regelverkLenker = sanityTiltakstype.regelverkLenker,
                    faneinnhold = sanityTiltakstype.faneinnhold,
                    delingMedBruker = sanityTiltakstype.delingMedBruker,
                    kanKombineresMed = sanityTiltakstype.kanKombineresMed,
                )
            }
        }
    }

    suspend fun hentOppskrifter(
        tiltakstypeId: UUID,
        perspective: SanityPerspective,
    ): List<Oppskrift> {
        return sanityService.getOppskrifter(tiltakstypeId, perspective)
    }

    suspend fun hentTiltaksgjennomforinger(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakstypeIds: List<String>? = null,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: ApentForPamelding = ApentForPamelding.APENT_ELLER_STENGT,
        search: String? = null,
        erSykmeldtMedArbeidsgiver: Boolean,
        cacheUsage: CacheUsage,
    ): List<VeilederflateTiltak> = coroutineScope {
        val individuelleGjennomforinger = async {
            hentSanityTiltak(enheter, tiltakstypeIds, innsatsgruppe, apentForPamelding, search, cacheUsage)
        }

        val gruppeGjennomforinger = async {
            hentGruppetiltak(
                enheter,
                tiltakstypeIds,
                innsatsgruppe,
                apentForPamelding,
                search,
                erSykmeldtMedArbeidsgiver,
            )
        }

        (individuelleGjennomforinger.await() + gruppeGjennomforinger.await())
    }

    suspend fun hentTiltaksgjennomforing(
        id: UUID,
        sanityPerspective: SanityPerspective,
        cacheUsage: CacheUsage,
    ): VeilederflateTiltak {
        return db.session { queries.veilderTiltak.get(id) }
            ?.let { gjennomforing ->
                toVeilederflateTiltak(gjennomforing)
                    ?: throw NotFoundException("Fant gjennomføring for id '$id'")
            }
            ?: run {
                val gjennomforing = sanityService.getTiltak(id, sanityPerspective, cacheUsage)
                toVeilederflateTiltak(gjennomforing)
            }
    }

    private suspend fun hentSanityTiltak(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakstypeIds: List<String>?,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: ApentForPamelding,
        search: String?,
        cacheUsage: CacheUsage,
    ): List<VeilederflateTiltak> {
        if (apentForPamelding == ApentForPamelding.STENGT) {
            // Det er foreløpig ikke noe egen funksjonalitet for å markere tiltak som midlertidig stengt i Sanity
            return emptyList()
        }

        val sanityGjennomforinger = sanityService.getAllTiltak(search, cacheUsage)

        val fylker = enheter.mapNotNull {
            navEnhetService.hentOverordnetFylkesenhet(it)?.enhetsnummer
        }

        return sanityGjennomforinger
            .filter { tiltakstypeIds.isNullOrEmpty() || tiltakstypeIds.contains(it.tiltakstype._id) }
            .map { toVeilederflateTiltak(it) }
            .filter { it.tiltakstype.innsatsgrupper.orEmpty().contains(innsatsgruppe) }
            .filter { gjennomforing ->
                if (gjennomforing.enheter.isEmpty()) {
                    gjennomforing.fylker.any { fylke -> fylke in fylker }
                } else {
                    gjennomforing.enheter.any { enhet -> enhet in enheter }
                }
            }
    }

    private suspend fun hentGruppetiltak(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakstypeIds: List<String>?,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: ApentForPamelding,
        search: String?,
        erSykmeldtMedArbeidsgiver: Boolean,
    ): List<VeilederflateTiltak> = db.session {
        return queries.veilderTiltak
            .getAll(
                search = search,
                sanityTiltakstypeIds = tiltakstypeIds?.map { UUID.fromString(it) },
                innsatsgruppe = innsatsgruppe,
                brukersEnheter = enheter,
                apentForPamelding = when (apentForPamelding) {
                    ApentForPamelding.APENT -> true
                    ApentForPamelding.STENGT -> false
                    ApentForPamelding.APENT_ELLER_STENGT -> null
                },
                erSykmeldtMedArbeidsgiver = erSykmeldtMedArbeidsgiver,
            )
            .mapNotNull { toVeilederflateTiltak(it) }
    }

    private suspend fun toVeilederflateTiltak(gjennomforing: Tiltaksgjennomforing): VeilederflateTiltak? {
        val tiltakstype = hentTiltakstyper().find { it.tiltakskode == gjennomforing.tiltakskode } ?: return null
        return VeilederflateTiltakGruppe(
            tiltakstype = tiltakstype,
            navn = gjennomforing.navn,
            beskrivelse = gjennomforing.beskrivelse,
            faneinnhold = gjennomforing.faneinnhold,
            kontaktinfo = gjennomforing.kontaktinfo,
            oppstart = gjennomforing.oppstart,
            oppmoteSted = gjennomforing.oppmoteSted,
            fylker = gjennomforing.fylker,
            enheter = gjennomforing.enheter,
            id = gjennomforing.id,
            status = gjennomforing.status,
            tiltaksnummer = gjennomforing.tiltaksnummer,
            apentForPamelding = gjennomforing.apentForPamelding,
            oppstartsdato = gjennomforing.oppstartsdato,
            sluttdato = gjennomforing.sluttdato,
            arrangor = gjennomforing.arrangor,
            estimertVentetid = gjennomforing.estimertVentetid,
            personvernBekreftet = gjennomforing.personvernBekreftet,
            personopplysningerSomKanBehandles = gjennomforing.personopplysningerSomKanBehandles,
        )
    }

    private suspend fun toVeilederflateTiltak(
        gjennomforing: SanityTiltaksgjennomforing,
    ): VeilederflateTiltak {
        val tiltakstype = hentTiltakstyper().single { it.sanityId == gjennomforing.tiltakstype._id }

        val tiltaksansvarlige = gjennomforing.kontaktpersoner
            ?.mapNotNull { it.navKontaktperson }
            ?.map {
                VeilederflateKontaktinfoTiltaksansvarlig(
                    navn = it.navn,
                    telefon = it.telefonnummer,
                    enhet = it.enhetsnummer?.let { enhet -> getTiltaksansvarligEnhet(enhet) },
                    epost = it.epost,
                    beskrivelse = it.beskrivelse,
                )
            } ?: emptyList()

        val arrangor = gjennomforing.arrangor?.let { arrangor ->
            val kontaktpersoner = arrangor.kontaktpersoner?.map { kontaktperson ->
                VeilederflateArrangorKontaktperson(
                    id = kontaktperson._id,
                    navn = kontaktperson.navn,
                    epost = kontaktperson.epost,
                    telefon = kontaktperson.telefon,
                    beskrivelse = kontaktperson.beskrivelse,
                )
            } ?: emptyList()
            VeilederflateArrangor(
                selskapsnavn = arrangor.navn,
                organisasjonsnummer = arrangor.organisasjonsnummer?.value,
                kontaktpersoner = kontaktpersoner,
            )
        }

        val navn = gjennomforing.tiltaksgjennomforingNavn ?: ""
        val faneinnhold = gjennomforing.faneinnhold?.copy(delMedBruker = gjennomforing.delingMedBruker)
        val kontaktinfo = VeilederflateKontaktinfo(tiltaksansvarlige)
        val fylker = listOfNotNull(gjennomforing.fylke)
        val enheter = gjennomforing.enheter?.filterNotNull() ?: emptyList()
        val tiltaksnummer = gjennomforing.tiltaksnummer
        val beskrivelse = gjennomforing.beskrivelse
        val sanityId = gjennomforing._id
        val stedForGjennomforing = gjennomforing.stedForGjennomforing

        return when {
            tiltakstype.arenakode != null && Tiltakskoder.isEgenRegiTiltak(tiltakstype.arenakode) -> {
                VeilederflateTiltakEgenRegi(
                    tiltaksnummer = tiltaksnummer,
                    beskrivelse = beskrivelse,
                    faneinnhold = faneinnhold,
                    kontaktinfo = kontaktinfo,
                    oppstart = GjennomforingOppstartstype.LOPENDE,
                    sanityId = sanityId,
                    tiltakstype = tiltakstype,
                    navn = navn,
                    oppmoteSted = stedForGjennomforing,
                    fylker = fylker,
                    enheter = enheter,
                )
            }

            arrangor != null -> VeilederflateTiltakEnkeltplassAnskaffet(
                tiltaksnummer = tiltaksnummer,
                beskrivelse = beskrivelse,
                faneinnhold = faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = GjennomforingOppstartstype.LOPENDE,
                sanityId = sanityId,
                tiltakstype = tiltakstype,
                navn = navn,
                fylker = fylker,
                enheter = enheter,
                arrangor = arrangor,
                oppmoteSted = stedForGjennomforing,
            )

            else -> VeilederflateTiltakEnkeltplass(
                beskrivelse = beskrivelse,
                faneinnhold = faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = GjennomforingOppstartstype.LOPENDE,
                sanityId = sanityId,
                tiltakstype = tiltakstype,
                navn = navn,
                fylker = fylker,
                enheter = enheter,
                oppmoteSted = stedForGjennomforing,
            )
        }
    }

    private fun getTiltaksansvarligEnhet(enhet: NavEnhetNummer): VeilederflateTiltaksansvarligHovedenhet? {
        return navEnhetService.hentEnhet(enhet)?.let {
            VeilederflateTiltaksansvarligHovedenhet(it.navn, it.enhetsnummer)
        }
    }
}
