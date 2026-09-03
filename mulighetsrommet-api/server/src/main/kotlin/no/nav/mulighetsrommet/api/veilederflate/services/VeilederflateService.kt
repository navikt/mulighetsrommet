package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.NonEmptyList
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.veilederflate.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.api.veilederflate.db.VeilederflateTiltakDokument
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
        apentForPamelding: List<ApentForPamelding>? = null,
        search: String? = null,
        erSykmeldtMedArbeidsgiver: Boolean,
    ): List<VeilederflateTiltak> = coroutineScope {
        val tiltakDokumenter = async {
            hentTiltakDokumenter(enheter, tiltakskoder, innsatsgruppe, apentForPamelding, search)
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

        (tiltakDokumenter.await() + gruppeGjennomforinger.await()).filter {
            tiltakstypeService.isEnabled(it.tiltakstype.tiltakskode, TiltakstypeFeature.VISES_I_MODIA)
        }
    }

    suspend fun hentTiltaksgjennomforing(id: UUID): VeilederflateTiltak {
        db.session { queries.veilderTiltak.get(id) }
            ?.let { return toVeilederflateTiltak(it) }

        db.session { queries.veilderTiltak.getTiltakDokument(id) }
            ?.let { return toVeilederflateTiltak(it) }

        throw NotFoundException("Fant ikke tiltak med id=$id")
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

    private suspend fun hentTiltakDokumenter(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakskoder: List<Tiltakskode>?,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: List<ApentForPamelding>?,
        search: String?,
    ): List<VeilederflateTiltak> {
        if (apentForPamelding?.toSet() == setOf(ApentForPamelding.STENGT)) {
            // Det er foreløpig ikke noe egen funksjonalitet for å markere tiltak som midlertidig stengt i Sanity
            return emptyList()
        }

        fun VeilederflateTiltak.searchableText() = buildList {
            add(navn)
            add(tiltakstype.navn)
            when (this@searchableText) {
                is VeilederflateTiltakGruppe -> Unit
                is VeilederflateTiltakEnkeltplass -> tiltaksnummer?.let { add(it) }
                is VeilederflateTiltakEnkeltplassAnskaffet -> tiltaksnummer?.let { add(it) }
            }
        }.joinToString(" ")

        // Hent publiserte rader fra vår database
        return db.session {
            queries.veilderTiltak.getAllTiltakDokument(
                brukersEnheter = enheter.toList(),
                tiltakskoder = tiltakskoder,
            )
        }
            .map { toVeilederflateTiltak(it) }
            .filter { it.tiltakstype.innsatsgrupper.orEmpty().contains(innsatsgruppe) }
            .filter { search == null || it.searchableText().contains(search, ignoreCase = true) }
    }

    private suspend fun hentGruppetiltak(
        enheter: NonEmptyList<NavEnhetNummer>,
        tiltakskoder: List<Tiltakskode>?,
        innsatsgruppe: Innsatsgruppe,
        apentForPamelding: List<ApentForPamelding>?,
        search: String?,
        erSykmeldtMedArbeidsgiver: Boolean,
    ): List<VeilederflateTiltak> = db.session {
        return queries.veilderTiltak
            .getAll(
                search = search,
                tiltakskoder = tiltakskoder,
                innsatsgruppe = innsatsgruppe,
                brukersEnheter = enheter,
                apentForPamelding = when {
                    apentForPamelding?.contains(ApentForPamelding.APENT) == true && apentForPamelding.contains(
                        ApentForPamelding.STENGT,
                    ) -> null

                    apentForPamelding?.contains(ApentForPamelding.APENT) == true -> true

                    apentForPamelding?.contains(ApentForPamelding.STENGT) == true -> false

                    else -> null
                },
                erSykmeldtMedArbeidsgiver = erSykmeldtMedArbeidsgiver,
            )
            .map { toVeilederflateTiltak(it) }
    }

    private suspend fun toVeilederflateTiltak(tiltakDokument: VeilederflateTiltakDokument): VeilederflateTiltak {
        val tiltakstype = getAllTiltakstyper().singleOrNull { it.tiltakskode == tiltakDokument.tiltakskode }
            ?: error("Tiltakstype mangler for tiltakskode=${tiltakDokument.tiltakskode}")

        val tiltaksansvarlige = tiltakDokument.kontaktpersoner.map {
            VeilederflateKontaktinfoTiltaksansvarlig(
                navn = it.navn,
                telefon = it.mobilnummer,
                epost = it.epost,
                beskrivelse = it.beskrivelse,
            )
        }
        val kontaktinfo = VeilederflateKontaktinfo(tiltaksansvarlige)

        val fylker = tiltakDokument.navEnheter.filter { it.type == NavEnhetType.FYLKE }.map { it.enhetsnummer }
        val enheter = tiltakDokument.navEnheter.filter { it.type != NavEnhetType.FYLKE }.map { it.enhetsnummer }

        return if (tiltakDokument.arrangor != null) {
            val arrangorKontaktpersoner = tiltakDokument.arrangorKontaktpersoner.map {
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
                navn = tiltakDokument.navn,
                beskrivelse = tiltakDokument.beskrivelse,
                faneinnhold = tiltakDokument.faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = GjennomforingOppstartstype.LOPENDE,
                oppmoteSted = tiltakDokument.stedForGjennomforing,
                fylker = fylker,
                enheter = enheter,
                id = tiltakDokument.id,
                tiltaksnummer = tiltakDokument.tiltaksnummer,
                arrangor = VeilederflateArrangor(
                    selskapsnavn = tiltakDokument.arrangor.navn,
                    organisasjonsnummer = tiltakDokument.arrangor.organisasjonsnummer,
                    kontaktpersoner = arrangorKontaktpersoner,
                ),
            )
        } else {
            VeilederflateTiltakEnkeltplass(
                tiltakstype = tiltakstype,
                navn = tiltakDokument.navn,
                beskrivelse = tiltakDokument.beskrivelse,
                faneinnhold = tiltakDokument.faneinnhold,
                kontaktinfo = kontaktinfo,
                oppstart = GjennomforingOppstartstype.LOPENDE,
                oppmoteSted = tiltakDokument.stedForGjennomforing,
                fylker = fylker,
                enheter = enheter,
                id = tiltakDokument.id,
                tiltaksnummer = tiltakDokument.tiltaksnummer,
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
}
