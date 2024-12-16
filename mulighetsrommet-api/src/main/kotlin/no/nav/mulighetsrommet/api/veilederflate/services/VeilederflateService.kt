package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.NonEmptyList
import io.ktor.server.plugins.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.services.cms.CacheUsage
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakRepository
import no.nav.mulighetsrommet.api.veilederflate.models.*
import no.nav.mulighetsrommet.api.veilederflate.routes.ApentForPamelding
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import java.util.*

class VeilederflateService(
    private val sanityService: SanityService,
    private val veilederflateTiltakRepository: VeilederflateTiltakRepository,
    private val tiltakstypeService: TiltakstypeService,
    private val navEnhetService: NavEnhetService,
) {
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
        return sanityService.getTiltakstyper()
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
                    arenakode = tiltakstype.arenaKode,
                    tiltakskode = tiltakstype.tiltakskode,
                    kanKombineresMed = it.kanKombineresMed,
                )
            }
    }

    suspend fun hentTiltaksgjennomforinger(
        enheter: NonEmptyList<String>,
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

    private suspend fun hentSanityTiltak(
        enheter: NonEmptyList<String>,
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

        val fylker = enheter.map {
            navEnhetService.hentOverordnetFylkesenhet(it)?.enhetsnummer
        }

        return sanityGjennomforinger
            .filter { tiltakstypeIds.isNullOrEmpty() || tiltakstypeIds.contains(it.tiltakstype._id) }
            .filter { it.tiltakstype.innsatsgrupper.contains(innsatsgruppe) }
            .map { toVeilederTiltaksgjennomforing(it) }
            .filter { gjennomforing ->
                if (gjennomforing.enheter.isEmpty()) {
                    gjennomforing.fylke in fylker
                } else {
                    gjennomforing.enheter.any { enhet -> enhet in enheter }
                }
            }
    }

    private fun hentGruppetiltak(
        enheter: NonEmptyList<String>,
        tiltakstypeIds: List<String>?,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: ApentForPamelding,
        search: String?,
        erSykmeldtMedArbeidsgiver: Boolean,
    ): List<VeilederflateTiltak> {
        return veilederflateTiltakRepository.getAll(
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
    }

    suspend fun hentTiltaksgjennomforing(
        id: UUID,
        sanityPerspective: SanityPerspective,
    ): VeilederflateTiltak {
        return veilederflateTiltakRepository.get(id)
            ?.let { gjennomforing ->
                val hentTiltakstyper = hentTiltakstyper()
                val sanityTiltakstype = hentTiltakstyper
                    .find { it.sanityId == gjennomforing.tiltakstype.sanityId }
                    ?: throw NotFoundException("Fant ikke tiltakstype for gjennomføring med id: '$id'")
                gjennomforing.copy(tiltakstype = sanityTiltakstype)
            }
            ?: run {
                val gjennomforing = sanityService.getTiltak(id, sanityPerspective)
                toVeilederTiltaksgjennomforing(gjennomforing)
            }
    }

    private fun toVeilederTiltaksgjennomforing(
        gjennomforing: SanityTiltaksgjennomforing,
    ): VeilederflateTiltak {
        val tiltakstypeAdminDto = tiltakstypeService.getBySanityId(UUID.fromString(gjennomforing.tiltakstype._id))

        val tiltaksansvarlige = gjennomforing.kontaktpersoner
            ?.mapNotNull { it.navKontaktperson }
            ?.map {
                VeilederflateKontaktinfoTiltaksansvarlig(
                    navn = it.navn,
                    telefon = it.telefonnummer,
                    enhet = it.enhet?.let { enhet -> navEnhetService.hentEnhet(enhet) },
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
                arrangorId = arrangor._id,
                selskapsnavn = arrangor.navn,
                organisasjonsnummer = arrangor.organisasjonsnummer?.value,
                kontaktpersoner = kontaktpersoner,
            )
        }

        val arenakode = tiltakstypeAdminDto.arenaKode
        val tiltakstype = gjennomforing.tiltakstype.run {
            VeilederflateTiltakstype(
                sanityId = _id,
                navn = tiltakstypeNavn,
                beskrivelse = beskrivelse,
                innsatsgrupper = innsatsgrupper,
                regelverkLenker = regelverkLenker,
                faneinnhold = faneinnhold,
                delingMedBruker = delingMedBruker,
                arenakode = arenakode,
                tiltakskode = tiltakstypeAdminDto.tiltakskode,
                kanKombineresMed = kanKombineresMed,
            )
        }

        val status = TiltaksgjennomforingStatus.GJENNOMFORES
        val tittel = tiltakstype.navn
        val underTittel = gjennomforing.tiltaksgjennomforingNavn ?: ""
        
        val faneinnhold = gjennomforing.faneinnhold?.copy(delMedBruker = gjennomforing.delingMedBruker)
        val kontaktinfo = VeilederflateKontaktinfo(tiltaksansvarlige)
        val fylke = gjennomforing.fylke ?: ""
        val enheter = gjennomforing.enheter?.filterNotNull() ?: emptyList()
        val tiltaksnummer = gjennomforing.tiltaksnummer
        val beskrivelse = gjennomforing.beskrivelse
        val sanityId = gjennomforing._id
        val stedForGjennomforing = gjennomforing.stedForGjennomforing

        return when {
            Tiltakskoder.isEgenRegiTiltak(arenakode) -> {
                VeilederflateTiltakEgenRegi(
                    tiltaksnummer = tiltaksnummer,
                    status = status,
                    beskrivelse = beskrivelse,
                    faneinnhold = faneinnhold,
                    kontaktinfo = kontaktinfo,
                    oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                    sanityId = sanityId,
                    tiltakstype = tiltakstype,
                    tittel = tittel,
                    underTittel = underTittel,
                    stedForGjennomforing = stedForGjennomforing,
                    fylke = fylke,
                    enheter = enheter,
                )
            }

            arrangor != null -> VeilederflateTiltakEnkeltplassAnskaffet(
                tiltaksnummer = tiltaksnummer,
                status = status,
                beskrivelse = beskrivelse,
                faneinnhold = faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                sanityId = sanityId,
                tiltakstype = tiltakstype,
                tittel = tittel,
                underTittel = underTittel,
                stedForGjennomforing = stedForGjennomforing,
                fylke = fylke,
                enheter = enheter,
                arrangor = arrangor,
            )

            else -> VeilederflateTiltakEnkeltplass(
                status = status,
                beskrivelse = beskrivelse,
                faneinnhold = faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                sanityId = sanityId,
                tiltakstype = tiltakstype,
                tittel = tittel,
                underTittel = underTittel,
                stedForGjennomforing = stedForGjennomforing,
                fylke = fylke,
                enheter = enheter,
            )
        }
    }

    suspend fun hentOppskrifter(
        tiltakstypeId: UUID,
        perspective: SanityPerspective,
    ): List<Oppskrift> {
        return sanityService.getOppskrifter(tiltakstypeId, perspective)
    }
}
