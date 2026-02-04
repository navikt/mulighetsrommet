package no.nav.tiltak.historikk.service

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.flatten
import arrow.core.mapOrAccumulate
import arrow.core.nonEmptySetOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.model.ArbeidsgiverAvtaleStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.tiltak.historikk.TiltakshistorikkMelding
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.TiltakshistorikkV1Request
import no.nav.tiltak.historikk.TiltakshistorikkV1Response
import no.nav.tiltak.historikk.clients.Avtale
import no.nav.tiltak.historikk.clients.GraphqlRequest
import no.nav.tiltak.historikk.clients.TiltakDatadelingClient
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.TiltakstypeDbo
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import no.nav.tiltak.historikk.util.Tiltaksnavn
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TiltakshistorikkService(
    private val db: TiltakshistorikkDatabase,
    private val tiltakDatadelingClient: TiltakDatadelingClient,
    private val cutOffDatoMapping: Map<Avtale.Tiltakstype, LocalDate>,
    private val virksomheter: VirksomhetService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getTiltakshistorikk(request: TiltakshistorikkV1Request): TiltakshistorikkV1Response = coroutineScope {
        val (identer, years) = request

        val arenaDeltakelser = async { getHistorikkArena(identer, years) }
        val teamKometDeltakelser = async { getHistorikkTeamKomet(identer, years) }
        val teamTiltakAvtaler = async { getHistorikkTeamTiltak(identer, years) }

        val deltakelser = arenaDeltakelser.await() + teamKometDeltakelser.await()

        teamTiltakAvtaler
            .await()
            .fold(
                { meldinger ->
                    val historikk = deltakelser.sortedWith(compareBy(nullsLast()) { it.startDato })
                    TiltakshistorikkV1Response(historikk = historikk, meldinger = meldinger)
                },
                { avtaler ->
                    val historikk = (deltakelser + avtaler).sortedWith(compareBy(nullsLast()) { it.startDato })
                    TiltakshistorikkV1Response(historikk = historikk, meldinger = setOf())
                },
            )
    }

    private fun getHistorikkTeamKomet(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
    ): List<TiltakshistorikkV1Dto.TeamKometDeltakelse> = db.session {
        queries.kometDeltaker.getKometHistorikk(identer, maxAgeYears)
    }

    private fun getHistorikkArena(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
    ): List<TiltakshistorikkV1Dto.ArenaDeltakelse> = db.session {
        val deltakelser = queries.arenaDeltaker.getArenaHistorikk(identer, maxAgeYears)

        deltakelser.filter { deltakelse ->
            val tiltakskode = arenaKodeToTeamTiltakKode(deltakelse.tiltakstype.tiltakskode) ?: return@filter true
            !belongsToTeamTiltak(tiltakskode, cutOffDatoMapping, deltakelse.sluttDato)
        }
    }

    private suspend fun getHistorikkTeamTiltak(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
    ): Either<NonEmptySet<TiltakshistorikkMelding>, List<TiltakshistorikkV1Dto.TeamTiltakAvtale>> {
        val minAvtaleDato = maxAgeYears?.let { LocalDate.now().minusYears(it.toLong()) } ?: LocalDate.MIN
        return identer
            .mapOrAccumulate {
                tiltakDatadelingClient.getAvtalerForPerson(
                    GraphqlRequest.GetAvtalerForPerson(norskIdent = it.value),
                    AccessType.M2M,
                ).bind()
            }
            .map { avtalerPerNorskIdent ->
                avtalerPerNorskIdent
                    .flatten()
                    .filter { avtale ->
                        belongsToTeamTiltak(avtale.tiltakstype, cutOffDatoMapping, avtale.sluttDato)
                    }
                    .filter { avtale ->
                        val avtaleDato = avtale.sluttDato
                            ?: avtale.startDato
                            ?: avtale.opprettetTidspunkt.toLocalDate()
                        !avtaleDato.isBefore(minAvtaleDato)
                    }
                    .map { avtale ->
                        val tiltakstype = getTiltakstype(avtale.tiltakstype)
                        val arbeidsgiver = getArbeidsgiver(avtale.bedriftNr)
                        toTiltakshistorikk(avtale, tiltakstype, arbeidsgiver)
                    }
            }
            .mapLeft { errors ->
                log.error("Klarte ikke hente tiltakshistorikk fra Team Tiltak. Errors=$errors")
                nonEmptySetOf(TiltakshistorikkMelding.MANGLER_HISTORIKK_FRA_TEAM_TILTAK)
            }
    }

    private suspend fun getArbeidsgiver(organisasjonsnummer: String): VirksomhetDbo? {
        return virksomheter.getOrSyncVirksomhetIfNotExists(Organisasjonsnummer(organisasjonsnummer))
            .onLeft { log.warn("Klarte ikke utlede arbeidsgiver for organisasjonsnummer=$organisasjonsnummer") }
            .getOrNull()
    }

    private fun getTiltakstype(tiltakskode: Avtale.Tiltakstype): TiltakstypeDbo = db.session {
        queries.tiltakstype.getByTiltakskode(tiltakskode)
    }
}

private fun belongsToTeamTiltak(
    tiltakstype: Avtale.Tiltakstype,
    cutOffDateMap: Map<Avtale.Tiltakstype, LocalDate>,
    sluttDato: LocalDate?,
): Boolean {
    val cutOffDate = cutOffDateMap[tiltakstype] ?: return false
    return sluttDato == null || sluttDato.isAfter(cutOffDate) || sluttDato == cutOffDate
}

private fun arenaKodeToTeamTiltakKode(arenaKode: String): Avtale.Tiltakstype? {
    return when (arenaKode) {
        "ARBTREN" -> Avtale.Tiltakstype.ARBEIDSTRENING
        "MIDLONTIL" -> Avtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD
        "VARLONTIL" -> Avtale.Tiltakstype.VARIG_LONNSTILSKUDD
        "MENTOR" -> Avtale.Tiltakstype.MENTOR
        "INKLUTILS" -> Avtale.Tiltakstype.INKLUDERINGSTILSKUDD
        "VATIAROR" -> Avtale.Tiltakstype.VTAO
        else -> null
    }
}

private fun toTiltakshistorikk(avtale: Avtale, tiltakstype: TiltakstypeDbo, arbeidsgiver: VirksomhetDbo?) = TiltakshistorikkV1Dto.TeamTiltakAvtale(
    norskIdent = avtale.deltakerFnr,
    startDato = avtale.startDato,
    sluttDato = avtale.sluttDato,
    id = avtale.avtaleId,
    tittel = Tiltaksnavn.hosTitleCaseVirksomhet(tiltakstype.navn, arbeidsgiver?.navn),
    tiltakstype = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype(
        tiltakskode = when (avtale.tiltakstype) {
            Avtale.Tiltakstype.ARBEIDSTRENING -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.ARBEIDSTRENING
            Avtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD
            Avtale.Tiltakstype.VARIG_LONNSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VARIG_LONNSTILSKUDD
            Avtale.Tiltakstype.MENTOR -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MENTOR
            Avtale.Tiltakstype.INKLUDERINGSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.INKLUDERINGSTILSKUDD
            Avtale.Tiltakstype.SOMMERJOBB -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.SOMMERJOBB
            Avtale.Tiltakstype.VTAO -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VTAO
        },
        navn = tiltakstype.navn,
    ),
    status = when (avtale.avtaleStatus) {
        Avtale.Status.ANNULLERT -> ArbeidsgiverAvtaleStatus.ANNULLERT
        Avtale.Status.AVBRUTT -> ArbeidsgiverAvtaleStatus.AVBRUTT
        Avtale.Status.PAABEGYNT -> ArbeidsgiverAvtaleStatus.PAABEGYNT
        Avtale.Status.MANGLER_GODKJENNING -> ArbeidsgiverAvtaleStatus.MANGLER_GODKJENNING
        Avtale.Status.KLAR_FOR_OPPSTART -> ArbeidsgiverAvtaleStatus.KLAR_FOR_OPPSTART
        Avtale.Status.GJENNOMFORES -> ArbeidsgiverAvtaleStatus.GJENNOMFORES
        Avtale.Status.AVSLUTTET -> ArbeidsgiverAvtaleStatus.AVSLUTTET
    },
    stillingsprosent = avtale.stillingprosent,
    dagerPerUke = avtale.antallDagerPerUke,
    arbeidsgiver = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer(avtale.bedriftNr), arbeidsgiver?.navn),
)
