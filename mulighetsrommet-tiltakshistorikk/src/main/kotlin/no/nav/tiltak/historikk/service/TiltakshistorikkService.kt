package no.nav.tiltak.historikk.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.TiltakshistorikkV1Request
import no.nav.tiltak.historikk.TiltakshistorikkV1Response
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.ArbeidsgiverAvtale
import no.nav.tiltak.historikk.model.Tiltakstype
import no.nav.tiltak.historikk.model.Virksomhet
import no.nav.tiltak.historikk.util.Tiltaksnavn
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TiltakshistorikkService(
    private val config: Config,
    private val db: TiltakshistorikkDatabase,
    private val virksomheter: VirksomhetService,
) {
    data class Config(
        val cutOffDatoMapping: Map<Tiltakskode, LocalDate>,
    )

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getTiltakshistorikk(request: TiltakshistorikkV1Request): TiltakshistorikkV1Response = coroutineScope {
        val (identer) = request

        val arenaDeltakelser = async { getHistorikkArena(identer) }
        val teamKometDeltakelser = async { getHistorikkTeamKomet(identer) }
        val teamTiltakAvtaler = async { getHistorikkTeamTiltak(identer) }

        val historikk = (arenaDeltakelser.await() + teamKometDeltakelser.await() + teamTiltakAvtaler.await())
            .sortedWith(compareBy(nullsLast()) { it.startDato })

        TiltakshistorikkV1Response(historikk = historikk, meldinger = setOf())
    }

    private fun getHistorikkTeamKomet(
        identer: List<NorskIdent>,
    ): List<TiltakshistorikkV1Dto.TeamKometDeltakelse> = db.session {
        queries.kometDeltaker.getKometHistorikk(identer)
    }

    private fun getHistorikkArena(
        identer: List<NorskIdent>,
    ): List<TiltakshistorikkV1Dto.ArenaDeltakelse> = db.session {
        val deltakelser = queries.arenaDeltaker.getArenaHistorikk(identer)

        deltakelser.filter { deltakelse ->
            val tiltakskode = arenaKodeToTeamTiltakKode(deltakelse.tiltakstype.tiltakskode) ?: return@filter true
            !belongsToTeamTiltak(tiltakskode, config.cutOffDatoMapping, deltakelse.sluttDato)
        }
    }

    private suspend fun getHistorikkTeamTiltak(
        identer: List<NorskIdent>,
    ): List<TiltakshistorikkV1Dto.TeamTiltakAvtale> {
        val avtaler = db.session {
            queries.arbeidsgiverAvtale.getByNorskIdent(identer)
        }

        return avtaler
            .filter { avtale ->
                belongsToTeamTiltak(avtale.tiltakstype, config.cutOffDatoMapping, avtale.sluttDato)
            }
            .map { avtale ->
                val tiltakstype = getTiltakstype(avtale.tiltakstype)
                val arbeidsgiver = getArbeidsgiver(avtale.organisasjonsnummer)
                toTiltakshistorikk(avtale, tiltakstype, arbeidsgiver)
            }
    }

    private suspend fun getArbeidsgiver(organisasjonsnummer: Organisasjonsnummer): Virksomhet? {
        return virksomheter.getOrSyncVirksomhetIfNotExists(organisasjonsnummer)
            .onLeft { log.warn("Klarte ikke utlede arbeidsgiver for organisasjonsnummer=${organisasjonsnummer.value}") }
            .getOrNull()
    }

    private fun getTiltakstype(tiltakskode: Tiltakskode): Tiltakstype = db.session {
        queries.tiltakstype.getByTiltakskode(tiltakskode)
    }
}

private fun belongsToTeamTiltak(
    tiltakstype: Tiltakskode,
    cutOffDateMap: Map<Tiltakskode, LocalDate>,
    sluttDato: LocalDate?,
): Boolean {
    val cutOffDate = cutOffDateMap[tiltakstype] ?: return false
    return sluttDato == null || sluttDato.isAfter(cutOffDate) || sluttDato == cutOffDate
}

private fun arenaKodeToTeamTiltakKode(arenaKode: String): Tiltakskode? {
    return when (arenaKode) {
        "ARBTREN" -> Tiltakskode.ARBEIDSTRENING
        "MIDLONTIL" -> Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD
        "VARLONTIL" -> Tiltakskode.VARIG_LONNSTILSKUDD
        "MENTOR" -> Tiltakskode.MENTOR
        "INKLUTILS" -> Tiltakskode.INKLUDERINGSTILSKUDD
        "VATIAROR" -> Tiltakskode.VTAO
        else -> null
    }
}

private fun toTiltakshistorikk(avtale: ArbeidsgiverAvtale, tiltakstype: Tiltakstype, arbeidsgiver: Virksomhet?) = TiltakshistorikkV1Dto.TeamTiltakAvtale(
    norskIdent = avtale.norskIdent,
    startDato = avtale.startDato,
    sluttDato = avtale.sluttDato,
    opprettetTidspunkt = avtale.opprettetTidspunkt,
    oppdatertTidspunkt = avtale.oppdatertTidspunkt,
    id = avtale.avtaleId,
    tittel = Tiltaksnavn.hosTitleCaseVirksomhet(tiltakstype.navn, arbeidsgiver?.navn),
    tiltakstype = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype(
        tiltakskode = when (avtale.tiltakstype) {
            Tiltakskode.ARBEIDSTRENING -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.ARBEIDSTRENING
            Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD
            Tiltakskode.VARIG_LONNSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VARIG_LONNSTILSKUDD
            Tiltakskode.MENTOR -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MENTOR
            Tiltakskode.INKLUDERINGSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.INKLUDERINGSTILSKUDD
            Tiltakskode.SOMMERJOBB -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.SOMMERJOBB
            Tiltakskode.VTAO -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VTAO
            Tiltakskode.FIREARIG_LONNSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.FIREARIG_LONNSTILSKUDD
            else -> throw IllegalStateException("${avtale.tiltakstype} er ikke støttet for avtale hos arbeidsgiver")
        },
        navn = tiltakstype.navn,
    ),
    status = when (avtale.status) {
        ArbeidsgiverAvtale.Status.ANNULLERT -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.ANNULLERT
        ArbeidsgiverAvtale.Status.AVBRUTT -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.AVBRUTT
        ArbeidsgiverAvtale.Status.PAABEGYNT -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.PAABEGYNT
        ArbeidsgiverAvtale.Status.MANGLER_GODKJENNING -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.MANGLER_GODKJENNING
        ArbeidsgiverAvtale.Status.KLAR_FOR_OPPSTART -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.KLAR_FOR_OPPSTART
        ArbeidsgiverAvtale.Status.GJENNOMFORES -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.GJENNOMFORES
        ArbeidsgiverAvtale.Status.AVSLUTTET -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.AVSLUTTET
    },
    stillingsprosent = avtale.stillingsprosent,
    dagerPerUke = avtale.dagerPerUke,
    arbeidsgiver = TiltakshistorikkV1Dto.Virksomhet(avtale.organisasjonsnummer, arbeidsgiver?.navn),
)
