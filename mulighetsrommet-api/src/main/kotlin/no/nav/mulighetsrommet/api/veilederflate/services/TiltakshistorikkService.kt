package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.getOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelseFraKomet
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserRequest
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.api.veilederflate.TiltaksnavnUtils.tittelOgUnderTittel
import no.nav.mulighetsrommet.api.veilederflate.hosTitleCaseArrangor
import no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val pdlClient: PdlClient,
    private val arrangorService: ArrangorService,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val tiltakstypeRepository: TiltakstypeRepository,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentHistorikk(norskIdent: NorskIdent, obo: AccessType.OBO): Deltakelser = coroutineScope {
        val historikk = async { getTiltakshistorikk(norskIdent, obo) }
        val deltakelser = async { getGruppetiltakDeltakelser(norskIdent, obo) }
        deltakelser.await().mergeWith(historikk.await())
    }

    private suspend fun getTiltakshistorikk(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Deltakelser = coroutineScope {
        val identer = hentHistoriskeNorskIdent(norskIdent, obo)
        val historikk = tiltakshistorikkClient.historikk(identer)

        val meldinger = historikk.meldinger
            .map {
                when (it) {
                    TiltakshistorikkMelding.MANGLER_HISTORIKK_FRA_TEAM_TILTAK -> DeltakelserMelding.MANGLER_DELTAKELSER_FRA_TEAM_TILTAK
                    TiltakshistorikkMelding.HENTER_IKKE_HISTORIKK_FRA_TEAM_TILTAK -> DeltakelserMelding.HENTER_IKKE_DELTAKELSER_FRA_TEAM_TILTAK
                }
            }
            .toSet()

        val (aktive, historiske) = historikk.historikk
            .map { async { toDeltakelse(it) } }
            .awaitAll()
            .partition { erAktiv(it) }

        Deltakelser(
            meldinger = meldinger,
            aktive = aktive,
            historiske = historiske,
        )
    }

    suspend fun getGruppetiltakDeltakelser(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Deltakelser {
        return amtDeltakerClient.hentDeltakelser(DeltakelserRequest(norskIdent), obo).fold({ error ->
            log.warn("Klarte ikke hente deltakelser fra Komet: $error")
            Deltakelser(
                meldinger = setOf(DeltakelserMelding.MANGLER_SISTE_DELTAKELSER_FRA_TEAM_KOMET),
                aktive = emptyList(),
                historiske = emptyList(),
            )
        }, { response ->
            Deltakelser(
                meldinger = setOf(),
                aktive = response.aktive.map { it.toDeltakelse() },
                historiske = response.historikk.map { it.toDeltakelse() },
            )
        })
    }

    private suspend fun toDeltakelse(it: Tiltakshistorikk) = when (it) {
        is Tiltakshistorikk.ArenaDeltakelse -> toDeltakelse(it)
        is Tiltakshistorikk.GruppetiltakDeltakelse -> toDeltakelse(it)
        is Tiltakshistorikk.ArbeidsgiverAvtale -> toDeltakelse(it)
    }

    private suspend fun toDeltakelse(deltakelse: Tiltakshistorikk.ArenaDeltakelse): Deltakelse.DeltakelseArena = coroutineScope {
        val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(deltakelse.arenaTiltakskode)
        val arrangorNavn = async { getArrangorHovedenhetNavn(deltakelse.arrangor.organisasjonsnummer) }

        val (tittel) = tittelOgUnderTittel(
            deltakelse.beskrivelse,
            tiltakstype.navn,
        )
        Deltakelse.DeltakelseArena(
            id = deltakelse.id,
            periode = Deltakelse.Periode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = Deltakelse.DeltakelseArena.Status(
                type = deltakelse.status,
                visningstekst = deltakelse.status.description,
            ),
            tittel = tittel.hosTitleCaseArrangor(arrangorNavn.await()),
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = Deltakelse.Eierskap.ARENA,
        )
    }

    private suspend fun toDeltakelse(deltakelse: Tiltakshistorikk.GruppetiltakDeltakelse) = coroutineScope {
        val tiltakstype = async { tiltakstypeRepository.getByTiltakskode(deltakelse.gjennomforing.tiltakskode) }
        val arrangorNavn = async { getArrangorHovedenhetNavn(deltakelse.arrangor.organisasjonsnummer) }

        val (tittel) = tittelOgUnderTittel(
            deltakelse.gjennomforing.navn,
            tiltakstype.await().navn,
        )
        Deltakelse.DeltakelseGruppetiltak(
            id = deltakelse.id,
            periode = Deltakelse.Periode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = Deltakelse.DeltakelseGruppetiltak.Status(
                type = deltakelse.status.type,
                visningstekst = deltakelse.status.type.description,
                aarsak = deltakelse.status.aarsak?.description,
            ),
            tittel = tittel.hosTitleCaseArrangor(arrangorNavn.await()),
            tiltakstypeNavn = tiltakstype.await().navn,
            innsoktDato = null,
            sistEndretDato = null,
            /**
             * Eierskapet er satt til ARENA selv om deltakelsene kommer fra Komet.
             * Det er først når deltakelsen også er tilgjengelig fra [AmtDeltakerClient.hentDeltakelser]
             * at eierskapet er TEAM_KOMET.
             */
            eierskap = Deltakelse.Eierskap.ARENA,
            gjennomforingId = deltakelse.gjennomforing.id,
        )
    }

    private suspend fun toDeltakelse(deltakelse: Tiltakshistorikk.ArbeidsgiverAvtale): Deltakelse.DeltakelseArbeidsgiverAvtale {
        val arenaKode = when (deltakelse.tiltakstype) {
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING -> "ARBTREN"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> "MIDLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_LONNSTILSKUDD -> "VARLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MENTOR -> "MENTOR"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.INKLUDERINGSTILSKUDD -> "INKLUTILS"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.SOMMERJOBB -> "TILSJOBB"
        }
        val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(arenaKode)
        val arrangorNavn = getArrangorNavn(deltakelse.arbeidsgiver.organisasjonsnummer)
        return Deltakelse.DeltakelseArbeidsgiverAvtale(
            id = deltakelse.avtaleId,
            periode = Deltakelse.Periode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = Deltakelse.DeltakelseArbeidsgiverAvtale.Status(
                type = deltakelse.status,
                visningstekst = deltakelse.status.description,
            ),
            tittel = tiltakstype.navn.hosTitleCaseArrangor(arrangorNavn),
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = Deltakelse.Eierskap.TEAM_TILTAK,
        )
    }

    private fun erAktiv(kort: Deltakelse): Boolean {
        return when (kort) {
            is Deltakelse.DeltakelseArena -> kort.status.type in listOf(
                ArenaDeltakerStatus.AKTUELL,
                ArenaDeltakerStatus.VENTELISTE,
                ArenaDeltakerStatus.TILBUD,
                ArenaDeltakerStatus.GJENNOMFORES,
                ArenaDeltakerStatus.INFORMASJONSMOTE,
                ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD,
            )

            is Deltakelse.DeltakelseGruppetiltak -> kort.status.type in listOf(
                DeltakerStatus.Type.VENTER_PA_OPPSTART,
                DeltakerStatus.Type.DELTAR,
                DeltakerStatus.Type.VURDERES,
                DeltakerStatus.Type.VENTELISTE,
                DeltakerStatus.Type.UTKAST_TIL_PAMELDING,
                DeltakerStatus.Type.SOKT_INN,
                DeltakerStatus.Type.PABEGYNT_REGISTRERING,
            )

            is Deltakelse.DeltakelseArbeidsgiverAvtale -> kort.status.type in listOf(
                ArbeidsgiverAvtaleStatus.PAABEGYNT,
                ArbeidsgiverAvtaleStatus.MANGLER_GODKJENNING,
                ArbeidsgiverAvtaleStatus.KLAR_FOR_OPPSTART,
                ArbeidsgiverAvtaleStatus.GJENNOMFORES,
            )
        }
    }

    private suspend fun hentHistoriskeNorskIdent(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): List<NorskIdent> {
        val request = GraphqlRequest.HentHistoriskeIdenter(
            ident = PdlIdent(norskIdent.value),
            grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT),
        )
        return pdlClient.hentHistoriskeIdenter(request, obo)
            .map { identer -> identer.map { NorskIdent(it.ident.value) } }
            .getOrElse {
                when (it) {
                    PdlError.Error -> throw Exception("Feil mot pdl!")
                    PdlError.NotFound -> listOf(norskIdent)
                }
            }
    }

    private suspend fun getArrangorHovedenhetNavn(orgnr: Organisasjonsnummer): String? {
        return arrangorService.getOrSyncArrangorFromBrreg(orgnr).fold({ error ->
            log.warn("Klarte ikke hente arrangørs hovedenhet. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.overordnetEnhet?.let { getArrangorHovedenhetNavn(it) } ?: virksomhet.navn
        })
    }

    private suspend fun getArrangorNavn(orgnr: Organisasjonsnummer): String? {
        return arrangorService.getOrSyncArrangorFromBrreg(orgnr).fold({ error ->
            log.warn("Klarte ikke hente hente arrangør. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.navn
        })
    }
}

fun DeltakelseFraKomet.toDeltakelse(): Deltakelse {
    return Deltakelse.DeltakelseGruppetiltak(
        id = deltakerId,
        gjennomforingId = deltakerlisteId,
        periode = Deltakelse.Periode(
            startDato = periode?.startdato,
            sluttDato = periode?.sluttdato,
        ),
        eierskap = Deltakelse.Eierskap.TEAM_KOMET,
        tittel = tittel,
        tiltakstypeNavn = tiltakstype.navn,
        status = Deltakelse.DeltakelseGruppetiltak.Status(
            type = status.type,
            visningstekst = status.visningstekst,
            aarsak = status.aarsak,
        ),
        innsoktDato = innsoktDato,
        sistEndretDato = sistEndretDato,
    )
}

enum class DeltakelserMelding {
    MANGLER_SISTE_DELTAKELSER_FRA_TEAM_KOMET,
    MANGLER_DELTAKELSER_FRA_TEAM_TILTAK,
    HENTER_IKKE_DELTAKELSER_FRA_TEAM_TILTAK,
}

data class Deltakelser(
    val meldinger: Set<DeltakelserMelding>,
    val aktive: List<Deltakelse>,
    val historiske: List<Deltakelse>,
) {
    /**
     * Kombinerer deltakelser. Ved konflikter på id så kastes deltakelse fra [other].
     */
    fun mergeWith(other: Deltakelser) = Deltakelser(
        meldinger = meldinger + other.meldinger,
        aktive = (aktive + other.aktive).distinctBy { it.id }.sortedWith(deltakelseComparator),
        historiske = (historiske + other.historiske).distinctBy { it.id }.sortedWith(deltakelseComparator),
    )
}

/**
 * Sorterer deltakelser basert på nyeste startdato først
 */
private val deltakelseComparator: Comparator<Deltakelse> = Comparator { a, b ->
    val startDatoA = a.periode.startDato
    val startDatoB = b.periode.startDato

    when {
        startDatoA === startDatoB -> 0
        startDatoA == null -> -1
        startDatoB == null -> 1
        else -> startDatoB.compareTo(startDatoA)
    }
}
