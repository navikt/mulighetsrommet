package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.getOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelseFraKomet
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserRequest
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.hosTitleCaseArrangor
import no.nav.mulighetsrommet.api.veilederflate.models.*
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentHistoriskeIdenterPdlQuery
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val historiskeIdenterQuery: HentHistoriskeIdenterPdlQuery,
    private val tiltakstypeService: TiltakstypeService,
    private val arrangorService: ArrangorService,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val tiltakshistorikkClient: TiltakshistorikkClient,
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

    private suspend fun toDeltakelse(deltakelse: Tiltakshistorikk.ArenaDeltakelse) = coroutineScope {
        val tiltakstype = async { tiltakstypeService.getByArenaTiltakskode(deltakelse.arenaTiltakskode) }
        val arrangorNavn = async { getArrangorHovedenhetNavn(deltakelse.arrangor.organisasjonsnummer) }

        DeltakelseArena(
            id = deltakelse.id,
            periode = DeltakelsePeriode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = DeltakelseArenaStatus(
                type = deltakelse.status,
                visningstekst = deltakelse.status.description,
            ),
            tittel = tiltakstype.await().navn.hosTitleCaseArrangor(arrangorNavn.await()),
            tiltakstypeNavn = tiltakstype.await().navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakelseEierskap.ARENA,
        )
    }

    private suspend fun toDeltakelse(deltakelse: Tiltakshistorikk.GruppetiltakDeltakelse) = coroutineScope {
        val tiltakstype = async { tiltakstypeService.getByTiltakskode(deltakelse.gjennomforing.tiltakskode) }
        val arrangorNavn = async { getArrangorHovedenhetNavn(deltakelse.arrangor.organisasjonsnummer) }

        DeltakelseGruppetiltak(
            id = deltakelse.id,
            periode = DeltakelsePeriode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = DeltakelseGruppetiltakStatus(
                type = deltakelse.status.type,
                visningstekst = deltakelse.status.type.description,
                aarsak = deltakelse.status.aarsak?.description,
            ),
            tittel = tiltakstype.await().navn.hosTitleCaseArrangor(arrangorNavn.await()),
            tiltakstypeNavn = tiltakstype.await().navn,
            innsoktDato = null,
            sistEndretDato = null,
            /**
             * Eierskapet er satt til ARENA selv om deltakelsene kommer fra Komet.
             * Det er først når deltakelsen også er tilgjengelig fra [AmtDeltakerClient.hentDeltakelser]
             * at eierskapet er TEAM_KOMET.
             */
            eierskap = DeltakelseEierskap.ARENA,
            gjennomforingId = deltakelse.gjennomforing.id,
        )
    }

    private suspend fun toDeltakelse(deltakelse: Tiltakshistorikk.ArbeidsgiverAvtale): DeltakelseArbeidsgiverAvtale {
        val arenaKode = when (deltakelse.tiltakstype) {
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING -> "ARBTREN"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> "MIDLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_LONNSTILSKUDD -> "VARLONTIL"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MENTOR -> "MENTOR"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.INKLUDERINGSTILSKUDD -> "INKLUTILS"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.SOMMERJOBB -> "TILSJOBB"
            Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_TILRETTELAGT_ARBEID_ORDINAR -> "VATIAROR"
        }
        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(arenaKode)
        val arrangorNavn = getArrangorNavn(deltakelse.arbeidsgiver.organisasjonsnummer)
        return DeltakelseArbeidsgiverAvtale(
            id = deltakelse.id,
            periode = DeltakelsePeriode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = DeltakelseArbeidsgiverAvtaleStatus(
                type = deltakelse.status,
                visningstekst = deltakelse.status.description,
            ),
            tittel = tiltakstype.navn.hosTitleCaseArrangor(arrangorNavn),
            tiltakstypeNavn = tiltakstype.navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakelseEierskap.TEAM_TILTAK,
        )
    }

    private fun erAktiv(kort: Deltakelse): Boolean {
        return when (kort) {
            is DeltakelseArena -> kort.status.type in listOf(
                ArenaDeltakerStatus.AKTUELL,
                ArenaDeltakerStatus.VENTELISTE,
                ArenaDeltakerStatus.TILBUD,
                ArenaDeltakerStatus.GJENNOMFORES,
                ArenaDeltakerStatus.INFORMASJONSMOTE,
                ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD,
            )

            is DeltakelseGruppetiltak -> kort.status.type in listOf(
                DeltakerStatusType.VENTER_PA_OPPSTART,
                DeltakerStatusType.DELTAR,
                DeltakerStatusType.VURDERES,
                DeltakerStatusType.VENTELISTE,
                DeltakerStatusType.UTKAST_TIL_PAMELDING,
                DeltakerStatusType.SOKT_INN,
                DeltakerStatusType.PABEGYNT_REGISTRERING,
            )

            is DeltakelseArbeidsgiverAvtale -> kort.status.type in listOf(
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
        return historiskeIdenterQuery.hentHistoriskeIdenter(request, obo)
            .map { identer -> identer.map { NorskIdent(it.ident.value) } }
            .getOrElse {
                when (it) {
                    PdlError.Error -> throw Exception("Feil mot pdl!")
                    PdlError.NotFound -> listOf(norskIdent)
                }
            }
    }

    private suspend fun getArrangorHovedenhetNavn(orgnr: Organisasjonsnummer): String? = retryOnException {
        arrangorService.getArrangorOrSyncFromBrreg(orgnr).fold({ error ->
            log.warn("Klarte ikke hente arrangørs hovedenhet: $orgnr. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.overordnetEnhet?.let { getArrangorHovedenhetNavn(it) } ?: virksomhet.navn
        })
    }

    private suspend fun getArrangorNavn(orgnr: Organisasjonsnummer): String? = retryOnException {
        arrangorService.getArrangorOrSyncFromBrreg(orgnr).fold({ error ->
            log.warn("Klarte ikke hente hente arrangør: $orgnr. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.navn
        })
    }

    private suspend fun <T> retryOnException(
        times: Int = 3,
        initialDelay: Long = 50,
        block: suspend () -> T,
    ): T {
        repeat(times - 1) { attempt ->
            val nextDelay = initialDelay * (attempt + 1)
            try {
                return block()
            } catch (e: Exception) {
                log.info("Exception oppsto under forsøk ${attempt + 1} av $times, venter $nextDelay ms før nytt forsøk. Feilmelding: ${e.message}")
                delay(nextDelay)
            }
        }

        return block()
    }
}

private fun DeltakelseFraKomet.toDeltakelse(): Deltakelse {
    return DeltakelseGruppetiltak(
        id = deltakerId,
        gjennomforingId = deltakerlisteId,
        periode = DeltakelsePeriode(
            startDato = periode?.startdato,
            sluttDato = periode?.sluttdato,
        ),
        eierskap = DeltakelseEierskap.TEAM_KOMET,
        tittel = tittel,
        tiltakstypeNavn = tiltakstype.navn,
        status = DeltakelseGruppetiltakStatus(
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
        startDatoA == startDatoB -> 0
        startDatoA == null -> -1
        startDatoB == null -> 1
        else -> startDatoB.compareTo(startDatoA)
    }
}
