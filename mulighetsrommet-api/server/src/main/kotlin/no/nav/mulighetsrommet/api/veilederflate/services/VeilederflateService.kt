package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.NonEmptyList
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.individuell_gjennomforing.model.IndividuellGjennomforing
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
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplass
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplassAnskaffet
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltaksansvarligHovedenhet
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.routes.ApentForPamelding
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utils.CachedComputation
import no.nav.mulighetsrommet.utils.toUUID
import java.time.Duration
import java.util.UUID

class VeilederflateService(
    private val db: ApiDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val sanityService: SanityService,
    private val navEnhetService: NavEnhetService,
) {
    private val cachedAllTiltakstyper = CachedComputation<List<VeilederflateTiltakstype>>(
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
        return getAllTiltakstyper().filter {
            tiltakstypeService.isEnabled(it.tiltakskode, TiltakstypeFeature.VISES_I_MODIA)
        }
    }

    suspend fun hentOppskrifter(
        tiltakskode: Tiltakskode,
        perspective: SanityPerspective,
    ): List<Oppskrift> {
        val sanityId = getAllTiltakstyper()
            .singleOrNull { it.tiltakskode == tiltakskode }
            ?.sanityId
            ?.toUUID()
            ?: return emptyList()
        return sanityService.getOppskrifter(sanityId, perspective)
    }

    suspend fun hentTiltaksgjennomforinger(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakskoder: List<Tiltakskode>? = null,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: ApentForPamelding = ApentForPamelding.APENT_ELLER_STENGT,
        search: String? = null,
        erSykmeldtMedArbeidsgiver: Boolean,
        cacheUsage: CacheUsage,
    ): List<VeilederflateTiltak> = coroutineScope {
        val individuelleGjennomforinger = async {
            hentSanityTiltak(enheter, tiltakskoder, innsatsgruppe, apentForPamelding, search, cacheUsage)
        }

        val gruppeGjennomforinger = async {
            hentGruppetiltak(
                enheter,
                tiltakskoder,
                innsatsgruppe,
                apentForPamelding,
                search,
                erSykmeldtMedArbeidsgiver,
            )
        }

        (individuelleGjennomforinger.await() + gruppeGjennomforinger.await()).filter {
            tiltakstypeService.isEnabled(it.tiltakstype.tiltakskode, TiltakstypeFeature.VISES_I_MODIA)
        }
    }

    suspend fun hentTiltaksgjennomforing(
        id: UUID,
        sanityPerspective: SanityPerspective,
        cacheUsage: CacheUsage,
    ): VeilederflateTiltak {
        db.session { queries.veilderTiltak.get(id) }
            ?.let { return toVeilederflateTiltak(it) }

        db.session { queries.individuellGjennomforing.get(id) }
            ?.let { return toVeilederflateTiltak(it) }

        db.session { queries.individuellGjennomforing.getBySanityId(id) }
            ?.let { return toVeilederflateTiltak(it) }

        val gjennomforing = sanityService.getTiltak(id, sanityPerspective, cacheUsage)
        return toVeilederflateTiltak(gjennomforing)
    }

    private suspend fun getAllTiltakstyper(): List<VeilederflateTiltakstype> {
        return cachedAllTiltakstyper.getOrCompute {
            db.session {
                repository.tiltakstype.getAll().map { tiltakstype ->
                    val veilederinfo = queries.tiltakstype.getVeilederinfo(tiltakstype.id)
                    VeilederflateTiltakstype(
                        sanityId = tiltakstype.sanityId?.toString(),
                        id = tiltakstype.id,
                        navn = tiltakstype.navn,
                        tiltakskode = tiltakstype.tiltakskode,
                        system = tiltakstype.tiltakskode.system,
                        features = tiltakstypeService.getFeatures(tiltakstype.tiltakskode),
                        egenskaper = tiltakstype.tiltakskode.egenskaper,
                        tiltaksgruppe = tiltakstype.tiltakskode.gruppe?.tittel,
                        innsatsgrupper = tiltakstype.innsatsgrupper,
                        beskrivelse = veilederinfo?.beskrivelse,
                        faneinnhold = veilederinfo?.faneinnhold,
                        faglenker = veilederinfo?.faglenker,
                        kanKombineresMed = veilederinfo?.kanKombineresMed ?: emptyList(),
                    )
                }
            }
        }
    }

    private suspend fun hentSanityTiltak(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakskoder: List<Tiltakskode>?,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: ApentForPamelding,
        search: String?,
        cacheUsage: CacheUsage,
    ): List<VeilederflateTiltak> {
        if (apentForPamelding == ApentForPamelding.STENGT) {
            // Det er foreløpig ikke noe egen funksjonalitet for å markere tiltak som midlertidig stengt i Sanity
            return emptyList()
        }

        val alleTiltakstyper = getAllTiltakstyper()

        val tiltakstypeIds = tiltakskoder?.let { koder ->
            alleTiltakstyper.filter { it.tiltakskode in koder }.map { it.id }
        }

        // Hent publiserte rader fra vår database
        val dbGjennomforinger = db.session {
            queries.individuellGjennomforing.getAll(
                navEnheter = enheter.toList(),
                tiltakstyper = tiltakstypeIds ?: emptyList(),
                publisert = true,
            )
        }

        // Sanity-IDer som finnes i databasen — disse skal ikke hentes fra Sanity i tillegg
        val sanityIderIDb = dbGjennomforinger.mapNotNull { it.sanityId }.toSet()

        val dbTiltak = dbGjennomforinger
            // Midlertidig filtrering av de som mangler tiltakstype. Det må støttes i Modia først
            .filter { it.tiltakstype != null }
            .map { toVeilederflateTiltak(it) }
            .filter { it.tiltakstype.innsatsgrupper.orEmpty().contains(innsatsgruppe) }

        val fylker = enheter.mapNotNull {
            navEnhetService.hentOverordnetFylkesenhet(it)?.enhetsnummer
        }

        val sanitySanityTiltakstypeIds = tiltakskoder?.let { koder ->
            alleTiltakstyper.filter { it.tiltakskode in koder }.map { it.sanityId }
        }

        val sanityTiltak = sanityService.getAllTiltak(search, cacheUsage)
            .filter { sanitySanityTiltakstypeIds.isNullOrEmpty() || sanitySanityTiltakstypeIds.contains(it.tiltakstype._id) }
            .filter { it._id.toUUID() !in sanityIderIDb }
            .map { toVeilederflateTiltak(it) }
            .filter { it.tiltakstype.innsatsgrupper.orEmpty().contains(innsatsgruppe) }
            .filter { gjennomforing ->
                if (gjennomforing.enheter.isEmpty()) {
                    gjennomforing.fylker.any { fylke -> fylke in fylker }
                } else {
                    gjennomforing.enheter.any { enhet -> enhet in enheter }
                }
            }

        return dbTiltak + sanityTiltak
    }

    private suspend fun hentGruppetiltak(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakskoder: List<Tiltakskode>?,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: ApentForPamelding,
        search: String?,
        erSykmeldtMedArbeidsgiver: Boolean,
    ): List<VeilederflateTiltak> = db.session {
        return queries.veilderTiltak
            .getAll(
                search = search,
                tiltakskoder = tiltakskoder,
                innsatsgruppe = innsatsgruppe,
                brukersEnheter = enheter,
                apentForPamelding = when (apentForPamelding) {
                    ApentForPamelding.APENT -> true
                    ApentForPamelding.STENGT -> false
                    ApentForPamelding.APENT_ELLER_STENGT -> null
                },
                erSykmeldtMedArbeidsgiver = erSykmeldtMedArbeidsgiver,
            )
            .map { toVeilederflateTiltak(it) }
    }

    private suspend fun toVeilederflateTiltak(gjennomforing: IndividuellGjennomforing): VeilederflateTiltak {
        val tiltakskode = gjennomforing.tiltakstype?.tiltakskode
            ?: error("IndividuellGjennomforing mangler tiltakstype: id=${gjennomforing.id}")
        val tiltakstype = getAllTiltakstyper().singleOrNull { it.tiltakskode == tiltakskode }
            ?: error("Tiltakstype mangler for tiltakskode=$tiltakskode")

        val fylker = gjennomforing.kontorstruktur.map { it.region.enhetsnummer }.distinct()
        val enheter = gjennomforing.kontorstruktur.flatMap { it.kontorer }.map { it.enhetsnummer }.distinct()

        val tiltaksansvarlige = gjennomforing.kontaktpersoner.map {
            VeilederflateKontaktinfoTiltaksansvarlig(
                navn = it.navn,
                telefon = it.mobilnummer,
                epost = it.epost,
                beskrivelse = it.beskrivelse,
            )
        }
        val kontaktinfo = VeilederflateKontaktinfo(tiltaksansvarlige)

        // Bruk sanityId som ekstern ID dersom det finnes, ellers bruk vår interne UUID
        val eksternId = (gjennomforing.sanityId ?: gjennomforing.id).toString()

        return if (gjennomforing.arrangor != null) {
            val arrangorKontaktpersoner = gjennomforing.arrangorKontaktpersoner.map {
                VeilederflateArrangorKontaktperson(
                    id = it.id,
                    navn = it.navn,
                    epost = it.epost ?: "",
                    telefon = it.telefon,
                    beskrivelse = it.beskrivelse,
                )
            }
            VeilederflateTiltakEnkeltplassAnskaffet(
                tiltakstype = tiltakstype,
                navn = gjennomforing.navn,
                beskrivelse = gjennomforing.beskrivelse,
                faneinnhold = gjennomforing.faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = GjennomforingOppstartstype.LOPENDE,
                oppmoteSted = gjennomforing.stedForGjennomforing,
                fylker = fylker,
                enheter = enheter,
                sanityId = eksternId,
                tiltaksnummer = gjennomforing.tiltaksnummer,
                arrangor = VeilederflateArrangor(
                    selskapsnavn = gjennomforing.arrangor.navn,
                    organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                    kontaktpersoner = arrangorKontaktpersoner,
                ),
            )
        } else {
            VeilederflateTiltakEnkeltplass(
                tiltakstype = tiltakstype,
                navn = gjennomforing.navn,
                beskrivelse = gjennomforing.beskrivelse,
                faneinnhold = gjennomforing.faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = GjennomforingOppstartstype.LOPENDE,
                oppmoteSted = gjennomforing.stedForGjennomforing,
                fylker = fylker,
                enheter = enheter,
                sanityId = eksternId,
                tiltaksnummer = gjennomforing.tiltaksnummer,
            )
        }
    }

    private suspend fun toVeilederflateTiltak(gjennomforing: Tiltaksgjennomforing): VeilederflateTiltak {
        val tiltakstype = getAllTiltakstyper().singleOrNull { it.tiltakskode == gjennomforing.tiltakskode } ?: error(
            "Tiltakstype mangler for tiltakskode=${gjennomforing.tiltakskode}",
        )

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
            apentForPamelding = gjennomforing.apentForPamelding,
            oppstartsdato = gjennomforing.oppstartsdato,
            sluttdato = gjennomforing.sluttdato,
            arrangor = gjennomforing.arrangor,
            estimertVentetid = gjennomforing.estimertVentetid,
            personvernBekreftet = gjennomforing.personvernBekreftet,
            personopplysningerSomKanBehandles = gjennomforing.personopplysningerSomKanBehandles,
            lopenummer = gjennomforing.lopenummer,
            stengtPerioder = gjennomforing.stengt,
        )
    }

    private suspend fun toVeilederflateTiltak(
        gjennomforing: SanityTiltaksgjennomforing,
    ): VeilederflateTiltak {
        val tiltakstype = getAllTiltakstyper().singleOrNull { it.sanityId == gjennomforing.tiltakstype._id } ?: error(
            "Tiltakstype mangler for sanityId=${gjennomforing.tiltakstype._id}",
        )

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
