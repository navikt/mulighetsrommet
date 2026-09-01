package no.nav.tiltak.historikk.service

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.flatten
import arrow.core.mapOrAccumulate
import arrow.core.nonEmptySetOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.tiltak.historikk.TiltakshistorikkMelding
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.TiltakshistorikkV1Request
import no.nav.tiltak.historikk.TiltakshistorikkV1Response
import no.nav.tiltak.historikk.clients.Avtale
import no.nav.tiltak.historikk.clients.GraphqlRequest
import no.nav.tiltak.historikk.clients.TiltakDatadelingClient
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
    private val tiltakDatadelingClient: TiltakDatadelingClient,
    private val virksomheter: VirksomhetService,
) {
    data class Config(
        val useKafkaForTeamTiltak: Boolean = false,
        val cutOffDatoMapping: Map<Avtale.Tiltakstype, LocalDate>,
    )

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getTiltakshistorikk(request: TiltakshistorikkV1Request): TiltakshistorikkV1Response = coroutineScope {
        val (identer) = request

        val arenaDeltakelser = async { getHistorikkArena(identer) }
        val teamKometDeltakelser = async { getHistorikkTeamKomet(identer) }
        val teamTiltakAvtaler = async { getHistorikkTeamTiltak(identer) }

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
    ): Either<NonEmptySet<TiltakshistorikkMelding>, List<TiltakshistorikkV1Dto.TeamTiltakAvtale>> = coroutineScope {
        if (config.useKafkaForTeamTiltak) {
            Either.Right(hentTeamTiltakAvtalerFraDb(identer))
        } else {
            val avtalerFraDbDeferred = async { hentTeamTiltakAvtalerFraDb(identer) }

            val resultatFraKlient = getHistorikkTeamTiltakFraKlient(identer)

            val avtalerFraDb = avtalerFraDbDeferred.await()
            resultatFraKlient.onRight { avtalerFraKlient ->
                loggDiffMellomKilderForTeamTiltak(avtalerFraDb, avtalerFraKlient)
            }

            resultatFraKlient
        }
    }

    private suspend fun hentTeamTiltakAvtalerFraDb(
        identer: List<NorskIdent>,
    ): List<TiltakshistorikkV1Dto.TeamTiltakAvtale> {
        val avtaler = db.session {
            queries.arbeidsgiverAvtale.getByNorskIdent(identer)
        }

        return avtaler
            .filter { avtale ->
                val tiltakstype = Avtale.Tiltakstype.valueOf(avtale.tiltakstype.name)
                belongsToTeamTiltak(tiltakstype, config.cutOffDatoMapping, avtale.sluttDato)
            }
            .map { avtale ->
                val tiltakstype = getTiltakstypeForKafkaAvtale(avtale)
                val arbeidsgiver = getArbeidsgiver(avtale.organisasjonsnummer)
                toTiltakshistorikk(avtale, tiltakstype, arbeidsgiver)
            }
    }

    private fun loggDiffMellomKilderForTeamTiltak(
        fraDb: List<TiltakshistorikkV1Dto.TeamTiltakAvtale>,
        fraKlient: List<TiltakshistorikkV1Dto.TeamTiltakAvtale>,
    ) {
        val dbIder = fraDb.map { it.id }.toSet()
        val klientIder = fraKlient.map { it.id }.toSet()

        val kunIDb = dbIder - klientIder
        val kunIKlient = klientIder - dbIder

        if (kunIDb.isNotEmpty() || kunIKlient.isNotEmpty()) {
            log.warn(
                "Fant differanse mellom tiltak fra Team Tiltak hentet fra db og fra TiltakDatadelingClient. " +
                    "Antall kun i db=${kunIDb.size}, antall kun i klient=${kunIKlient.size}. " +
                    "Id-er kun i db=$kunIDb, id-er kun i klient=$kunIKlient",
            )
        }
    }

    private suspend fun getHistorikkTeamTiltakFraKlient(
        identer: List<NorskIdent>,
    ): Either<NonEmptySet<TiltakshistorikkMelding>, List<TiltakshistorikkV1Dto.TeamTiltakAvtale>> {
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
                        belongsToTeamTiltak(avtale.tiltakstype, config.cutOffDatoMapping, avtale.sluttDato)
                    }
                    .map { avtale ->
                        val tiltakstype = getTiltakstype(avtale.tiltakstype)
                        val arbeidsgiver = getArbeidsgiver(Organisasjonsnummer(avtale.bedriftNr))
                        toTiltakshistorikk(avtale, tiltakstype, arbeidsgiver)
                    }
            }
            .mapLeft { errors ->
                log.error("Klarte ikke hente tiltakshistorikk fra Team Tiltak. Errors=$errors")
                nonEmptySetOf(TiltakshistorikkMelding.MANGLER_HISTORIKK_FRA_TEAM_TILTAK)
            }
    }

    private suspend fun getArbeidsgiver(organisasjonsnummer: Organisasjonsnummer): Virksomhet? {
        return virksomheter.getOrSyncVirksomhetIfNotExists(organisasjonsnummer)
            .onLeft { log.warn("Klarte ikke utlede arbeidsgiver for organisasjonsnummer=${organisasjonsnummer.value}") }
            .getOrNull()
    }

    private fun getTiltakstype(tiltakskode: Avtale.Tiltakstype): Tiltakstype = db.session {
        queries.tiltakstype.getByTiltakskode(tiltakskode)
    }

    private fun getTiltakstypeForKafkaAvtale(avtale: ArbeidsgiverAvtale): Tiltakstype = db.session {
        val tiltakskode = Avtale.Tiltakstype.valueOf(avtale.tiltakstype.name)
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

private fun toTiltakshistorikk(avtale: Avtale, tiltakstype: Tiltakstype, arbeidsgiver: Virksomhet?) = TiltakshistorikkV1Dto.TeamTiltakAvtale(
    norskIdent = avtale.deltakerFnr,
    startDato = avtale.startDato,
    sluttDato = avtale.sluttDato,
    opprettetTidspunkt = avtale.opprettetTidspunkt.toInstant(),
    oppdatertTidspunkt = avtale.endretTidspunkt.toInstant(),
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
            Avtale.Tiltakstype.FIREARIG_LONNSTILSKUDD -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.FIREARIG_LONNSTILSKUDD
        },
        navn = tiltakstype.navn,
    ),
    status = when (avtale.avtaleStatus) {
        Avtale.Status.ANNULLERT -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.ANNULLERT
        Avtale.Status.AVBRUTT -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.AVBRUTT
        Avtale.Status.PAABEGYNT -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.PAABEGYNT
        Avtale.Status.MANGLER_GODKJENNING -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.MANGLER_GODKJENNING
        Avtale.Status.KLAR_FOR_OPPSTART -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.KLAR_FOR_OPPSTART
        Avtale.Status.GJENNOMFORES -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.GJENNOMFORES
        Avtale.Status.AVSLUTTET -> TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.AVSLUTTET
    },
    stillingsprosent = avtale.stillingprosent,
    dagerPerUke = avtale.antallDagerPerUke,
    arbeidsgiver = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer(avtale.bedriftNr), arbeidsgiver?.navn),
)
