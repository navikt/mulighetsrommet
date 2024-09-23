package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.getOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.clients.amtDeltaker.*
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.domain.dto.DeltakerKort
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.TiltaksnavnUtils
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
    ): Deltakelser {
        val identer = hentHistoriskeNorskIdent(norskIdent, obo)
        val historikk = tiltakshistorikkClient.historikk(identer)

        val meldinger = historikk.meldinger
            .map {
                when (it) {
                    TiltakshistorikkMelding.MANGLER_HISTORIKK_FRA_TEAM_TILTAK -> DeltakelserMelding.MANGLER_DELTAKELSER_FRA_TEAM_TILTAK
                }
            }
            .toSet()

        val (aktive, historiske) = historikk.historikk
            .map { toDeltakerKort(it) }
            .partition { erAktiv(it.status.type) }

        return Deltakelser(
            meldinger = meldinger,
            aktive = aktive,
            historiske = historiske,
        )
    }

    private suspend fun getGruppetiltakDeltakelser(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Deltakelser {
        if (NaisEnv.current().isProdGCP()) {
            log.debug("Henter ikke deltakelser fra Komet sitt API i prod")
            return Deltakelser(
                setOf(DeltakelserMelding.MANGLER_SISTE_DELTAKELSER_FRA_TEAM_KOMET),
                emptyList(),
                emptyList(),
            )
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
                aktive = response.aktive.map { it.toDeltakerKort() },
                historiske = response.historikk.map { it.toDeltakerKort() },
            )
        })
    }

    private suspend fun toDeltakerKort(it: Tiltakshistorikk) = when (it) {
        is Tiltakshistorikk.ArenaDeltakelse -> {
            it.toDeltakerKort()
        }

        is Tiltakshistorikk.GruppetiltakDeltakelse -> {
            it.toDeltakerKort()
        }

        is Tiltakshistorikk.ArbeidsgiverAvtale -> it.toDeltakerKort()
    }

    private fun Tiltakshistorikk.ArenaDeltakelse.toDeltakerKort(): DeltakerKort {
        val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(arenaTiltakskode)
        return DeltakerKort(
            id = id,
            periode = DeltakerKort.Periode(
                startDato = startDato,
                sluttDato = sluttDato,
            ),
            status = DeltakerKort.DeltakerStatus(
                type = DeltakerKort.DeltakerStatus.DeltakerStatusType.valueOf(status.name),
                visningstekst = arenaStatusTilVisningstekst(status),
                aarsak = null,
            ),
            tittel = beskrivelse,
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakerKort.Eierskap.ARENA,
        )
    }

    private suspend fun Tiltakshistorikk.GruppetiltakDeltakelse.toDeltakerKort(): DeltakerKort {
        val tiltakstype = tiltakstypeRepository.getByTiltakskode(gjennomforing.tiltakskode)
        val arrangorNavn = getArrangorHovedenhet(arrangor.organisasjonsnummer).navn
        return DeltakerKort(
            id = id,
            periode = DeltakerKort.Periode(
                startDato = startDato,
                sluttDato = sluttDato,
            ),
            status = DeltakerKort.DeltakerStatus(
                type = DeltakerKort.DeltakerStatus.DeltakerStatusType.valueOf(status.type.name),
                visningstekst = gruppetiltakStatusTilVisningstekst(status.type),
                aarsak = gruppetiltakAarsakTilTekst(status.aarsak),
            ),
            tittel = TiltaksnavnUtils.tilKonstruertNavn(tiltakstype, arrangorNavn),
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakerKort.Eierskap.ARENA,
        )
    }

    private suspend fun Tiltakshistorikk.ArbeidsgiverAvtale.toDeltakerKort(): DeltakerKort {
        val arenaKode = when (tiltakstype) {
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING -> "ARBTREN"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> "MIDLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_LONNSTILSKUDD -> "VARLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MENTOR -> "MENTOR"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.INKLUDERINGSTILSKUDD -> "INKLUTILS"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.SOMMERJOBB -> "TILSJOBB"
        }
        val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(arenaKode)
        val arrangorNavn = getArrangor(arbeidsgiver.organisasjonsnummer).navn
        return DeltakerKort(
            id = avtaleId,
            periode = DeltakerKort.Periode(
                startDato = startDato,
                sluttDato = sluttDato,
            ),
            status = DeltakerKort.DeltakerStatus(
                type = DeltakerKort.DeltakerStatus.DeltakerStatusType.valueOf(status.name),
                visningstekst = arbeidsgiverAvtaleStatusTilVisningstekst(status),
                aarsak = null,
            ),
            tittel = TiltaksnavnUtils.tilKonstruertNavn(tiltakstype, arrangorNavn),
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakerKort.Eierskap.TEAM_TILTAK,
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

    private fun erAktiv(status: DeltakerKort.DeltakerStatus.DeltakerStatusType): Boolean {
        return when (status) {
            DeltakerKort.DeltakerStatus.DeltakerStatusType.AKTUELL,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.VENTER_PA_OPPSTART,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.DELTAR,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.VURDERES,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.VENTELISTE,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.UTKAST_TIL_PAMELDING,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.SOKT_INN,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.TILBUD,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.KLADD,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.GJENNOMFORES,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.INFORMASJONSMOTE,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.TAKKET_JA_TIL_TILBUD,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.PABEGYNT_REGISTRERING,
            -> true

            else -> false
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

    private suspend fun getArrangorHovedenhet(orgnr: Organisasjonsnummer): TiltakshistorikkDto.Arrangor {
        val navn = arrangorService.getOrSyncArrangorFromBrreg(orgnr.value).fold({ error ->
            log.warn("Klarte ikke hente arrangørs hovedenhet. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.overordnetEnhet?.let { getArrangorHovedenhet(Organisasjonsnummer(it)) }?.navn ?: virksomhet.navn
        })

        return TiltakshistorikkDto.Arrangor(organisasjonsnummer = Organisasjonsnummer(orgnr.value), navn = navn)
    }

    private suspend fun getArrangor(orgnr: Organisasjonsnummer): TiltakshistorikkDto.Arrangor {
        val navn = arrangorService.getOrSyncArrangorFromBrreg(orgnr.value).fold({ error ->
            log.warn("Klarte ikke hente hente arrangør. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.navn
        })

        return TiltakshistorikkDto.Arrangor(organisasjonsnummer = Organisasjonsnummer(orgnr.value), navn = navn)
    }
}

fun DeltakelseFraKomet.toDeltakerKort(): DeltakerKort {
    return DeltakerKort(
        id = deltakerId,
        tiltaksgjennomforingId = deltakerlisteId,
        periode = DeltakerKort.Periode(
            startDato = periode?.startdato,
            sluttDato = periode?.sluttdato,
        ),
        eierskap = DeltakerKort.Eierskap.KOMET,
        tittel = tittel,
        tiltakstypeNavn = tiltakstype.navn,
        status = DeltakerKort.DeltakerStatus(
            type = DeltakerKort.DeltakerStatus.DeltakerStatusType.valueOf(status.type.name),
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
}

data class Deltakelser(
    val meldinger: Set<DeltakelserMelding>,
    val aktive: List<DeltakerKort>,
    val historiske: List<DeltakerKort>,
) {
    /**
     * Kombinerer deltakelser. Ved konflikter på id så kastes deltakelse fra [other].
     */
    fun mergeWith(other: Deltakelser) = Deltakelser(
        meldinger = meldinger + other.meldinger,
        aktive = (aktive + other.aktive).distinctBy { it.id }.sortedWith(deltakerKortComparator),
        historiske = (historiske + other.historiske).distinctBy { it.id }.sortedWith(deltakerKortComparator),
    )
}

/**
 * Sorterer deltakelser basert på nyeste startdato først
 */
private val deltakerKortComparator: Comparator<DeltakerKort> = Comparator { a, b ->
    val startDatoA = a.periode.startDato
    val startDatoB = b.periode.startDato

    when {
        startDatoA === startDatoB -> 0
        startDatoA == null -> -1
        startDatoB == null -> 1
        else -> startDatoB.compareTo(startDatoA)
    }
}
