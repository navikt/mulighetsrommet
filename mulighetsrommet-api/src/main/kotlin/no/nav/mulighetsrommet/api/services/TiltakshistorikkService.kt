package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.getOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.clients.amtDeltaker.*
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.domain.dto.Deltakelse
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.TiltaksnavnUtils.hosTitleCaseArrangor
import no.nav.mulighetsrommet.api.utils.TiltaksnavnUtils.tittelOgUnderTittel
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkMelding
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.env.NaisEnv
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
        if (NaisEnv.current().isProdGCP()) {
            log.debug("Henter ikke deltakelser fra Komet sitt API i prod")
            return Deltakelser(setOf(), emptyList(), emptyList())
        }

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
        is Tiltakshistorikk.ArenaDeltakelse -> it.toDeltakelse()
        is Tiltakshistorikk.GruppetiltakDeltakelse -> it.toDeltakelse()
        is Tiltakshistorikk.ArbeidsgiverAvtale -> it.toDeltakelse()
    }

    private fun Tiltakshistorikk.ArenaDeltakelse.toDeltakelse(): Deltakelse.DeltakelseArena {
        val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(arenaTiltakskode)
        return Deltakelse.DeltakelseArena(
            id = id,
            periode = Deltakelse.Periode(
                startDato = startDato,
                sluttDato = sluttDato,
            ),
            status = Deltakelse.DeltakelseArena.DeltakerStatus(
                type = status,
                visningstekst = arenaStatusTilVisningstekst(status),
            ),
            tittel = beskrivelse,
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = Deltakelse.Eierskap.ARENA,
        )
    }

    private suspend fun Tiltakshistorikk.GruppetiltakDeltakelse.toDeltakelse() = coroutineScope {
        val tiltakstype = async { tiltakstypeRepository.getByTiltakskode(gjennomforing.tiltakskode) }
        val arrangorNavn = async { getArrangorHovedenhetNavn(arrangor.organisasjonsnummer) }

        val (tittel) = tittelOgUnderTittel(
            gjennomforing.navn,
            tiltakstype.await().navn,
            gjennomforing.tiltakskode,
        )
        Deltakelse.DeltakelseGruppetiltak(
            id = id,
            periode = Deltakelse.Periode(
                startDato = startDato,
                sluttDato = sluttDato,
            ),
            status = Deltakelse.DeltakelseGruppetiltak.DeltakerStatus(
                type = status.type,
                visningstekst = gruppetiltakStatusTilVisningstekst(status.type),
                aarsak = gruppetiltakAarsakTilTekst(status.aarsak),
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
            gjennomforingId = gjennomforing.id,
        )
    }

    private suspend fun Tiltakshistorikk.ArbeidsgiverAvtale.toDeltakelse(): Deltakelse.DeltakelseArbeidsgiverAvtale {
        val arenaKode = when (tiltakstype) {
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING -> "ARBTREN"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> "MIDLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_LONNSTILSKUDD -> "VARLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MENTOR -> "MENTOR"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.INKLUDERINGSTILSKUDD -> "INKLUTILS"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.SOMMERJOBB -> "TILSJOBB"
        }
        val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(arenaKode)
        val arrangorNavn = getArrangorNavn(arbeidsgiver.organisasjonsnummer)
        return Deltakelse.DeltakelseArbeidsgiverAvtale(
            id = avtaleId,
            periode = Deltakelse.Periode(
                startDato = startDato,
                sluttDato = sluttDato,
            ),
            status = Deltakelse.DeltakelseArbeidsgiverAvtale.DeltakerStatus(
                type = status,
                visningstekst = arbeidsgiverAvtaleStatusTilVisningstekst(status),
            ),
            tittel = tiltakstype.navn.hosTitleCaseArrangor(arrangorNavn),
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = Deltakelse.Eierskap.TEAM_TILTAK,
        )
    }

    private fun gruppetiltakAarsakTilTekst(aarsak: AmtDeltakerStatus.Aarsak?): String? {
        return when (aarsak) {
            AmtDeltakerStatus.Aarsak.SYK -> "Syk"
            AmtDeltakerStatus.Aarsak.FATT_JOBB -> "Fått jobb"
            AmtDeltakerStatus.Aarsak.TRENGER_ANNEN_STOTTE -> "Trenger annen støtte"
            AmtDeltakerStatus.Aarsak.FIKK_IKKE_PLASS -> "Fikk ikke plass"
            AmtDeltakerStatus.Aarsak.IKKE_MOTT -> "Møter ikke opp"
            AmtDeltakerStatus.Aarsak.ANNET -> "Annet"
            AmtDeltakerStatus.Aarsak.AVLYST_KONTRAKT -> "Avlyst kontrakt"
            AmtDeltakerStatus.Aarsak.UTDANNING -> "Utdanning"
            AmtDeltakerStatus.Aarsak.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT -> "Samarbeidet med arrangøren er avbrutt"
            AmtDeltakerStatus.Aarsak.FERDIG -> "Ferdig"
            AmtDeltakerStatus.Aarsak.FEILREGISTRERT -> "Feilregistrert"
            AmtDeltakerStatus.Aarsak.OPPFYLLER_IKKE_KRAVENE -> "Oppfyller ikke kravene"
            null -> null
        }
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
                AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
                AmtDeltakerStatus.Type.DELTAR,
                AmtDeltakerStatus.Type.VURDERES,
                AmtDeltakerStatus.Type.VENTELISTE,
                AmtDeltakerStatus.Type.UTKAST_TIL_PAMELDING,
                AmtDeltakerStatus.Type.SOKT_INN,
                AmtDeltakerStatus.Type.PABEGYNT_REGISTRERING,
            )

            is Deltakelse.DeltakelseArbeidsgiverAvtale -> kort.status.type in listOf(
                Tiltakshistorikk.ArbeidsgiverAvtale.Status.PAABEGYNT,
                Tiltakshistorikk.ArbeidsgiverAvtale.Status.MANGLER_GODKJENNING,
                Tiltakshistorikk.ArbeidsgiverAvtale.Status.KLAR_FOR_OPPSTART,
                Tiltakshistorikk.ArbeidsgiverAvtale.Status.GJENNOMFORES,
            )
        }
    }

    private fun gruppetiltakStatusTilVisningstekst(status: AmtDeltakerStatus.Type): String {
        return when (status) {
            AmtDeltakerStatus.Type.FULLFORT -> "Fullført"
            AmtDeltakerStatus.Type.VENTER_PA_OPPSTART -> "Venter på oppstart"
            AmtDeltakerStatus.Type.DELTAR -> "Deltar"
            AmtDeltakerStatus.Type.HAR_SLUTTET -> "Har sluttet"
            AmtDeltakerStatus.Type.IKKE_AKTUELL -> "Ikke aktuell"
            AmtDeltakerStatus.Type.FEILREGISTRERT -> "Feilregistrert"
            AmtDeltakerStatus.Type.PABEGYNT_REGISTRERING -> "Påbegynt registrering"
            AmtDeltakerStatus.Type.SOKT_INN -> "Søkt om plass"
            AmtDeltakerStatus.Type.VURDERES -> "Vurderes"
            AmtDeltakerStatus.Type.VENTELISTE -> "På venteliste"
            AmtDeltakerStatus.Type.AVBRUTT -> "Avbrutt"
            AmtDeltakerStatus.Type.UTKAST_TIL_PAMELDING -> "Utkast til påmelding"
            AmtDeltakerStatus.Type.AVBRUTT_UTKAST -> "Avbrutt utkast"
            AmtDeltakerStatus.Type.KLADD -> "Kladd"
        }
    }

    private fun arenaStatusTilVisningstekst(status: ArenaDeltakerStatus): String {
        return when (status) {
            ArenaDeltakerStatus.AVSLAG -> "Fått avslag"
            ArenaDeltakerStatus.IKKE_AKTUELL -> "Ikke aktuell"
            ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD -> "Takket nei til tilbud"
            ArenaDeltakerStatus.TILBUD -> "Godkjent tiltaksplass"
            ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD -> "Takket ja til tilbud"
            ArenaDeltakerStatus.INFORMASJONSMOTE -> "Informasjonsmøte"
            ArenaDeltakerStatus.AKTUELL -> "Aktuell"
            ArenaDeltakerStatus.VENTELISTE -> "Venteliste"
            ArenaDeltakerStatus.GJENNOMFORES -> "Gjennomføres"
            ArenaDeltakerStatus.DELTAKELSE_AVBRUTT -> "Deltakelse avbrutt"
            ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT -> "Gjennomføring avbrutt"
            ArenaDeltakerStatus.GJENNOMFORING_AVLYST -> "Gjennomføring avlyst"
            ArenaDeltakerStatus.FULLFORT -> "Fullført"
            ArenaDeltakerStatus.IKKE_MOTT -> "Ikke møtt"
            ArenaDeltakerStatus.FEILREGISTRERT -> "Feilregistrert"
        }
    }

    private fun arbeidsgiverAvtaleStatusTilVisningstekst(status: Tiltakshistorikk.ArbeidsgiverAvtale.Status): String {
        return when (status) {
            Tiltakshistorikk.ArbeidsgiverAvtale.Status.ANNULLERT -> "Annullert"
            Tiltakshistorikk.ArbeidsgiverAvtale.Status.AVBRUTT -> "Avbrutt"
            Tiltakshistorikk.ArbeidsgiverAvtale.Status.PAABEGYNT -> "Påbegynt"
            Tiltakshistorikk.ArbeidsgiverAvtale.Status.MANGLER_GODKJENNING -> "Mangler godkjenning"
            Tiltakshistorikk.ArbeidsgiverAvtale.Status.KLAR_FOR_OPPSTART -> "Klar for oppstart"
            Tiltakshistorikk.ArbeidsgiverAvtale.Status.GJENNOMFORES -> "Gjennomføres"
            Tiltakshistorikk.ArbeidsgiverAvtale.Status.AVSLUTTET -> "Avsluttet"
        }
    }

    suspend fun hentDeltakelserFraKomet(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Either<AmtDeltakerError, DeltakelserResponse> {
        if (NaisEnv.current().isProdGCP()) {
            log.debug("Henter ikke deltakelser fra Komet sitt API i prod")
            return Either.Right(DeltakelserResponse(emptyList(), emptyList()))
        }

        return amtDeltakerClient.hentDeltakelser(DeltakelserRequest(norskIdent), obo)
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
        return arrangorService.getOrSyncArrangorFromBrreg(orgnr.value).fold({ error ->
            log.warn("Klarte ikke hente arrangørs hovedenhet. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.overordnetEnhet?.let { getArrangorHovedenhetNavn(Organisasjonsnummer(it)) } ?: virksomhet.navn
        })
    }

    private suspend fun getArrangorNavn(orgnr: Organisasjonsnummer): String? {
        return arrangorService.getOrSyncArrangorFromBrreg(orgnr.value).fold({ error ->
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
        status = Deltakelse.DeltakelseGruppetiltak.DeltakerStatus(
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
