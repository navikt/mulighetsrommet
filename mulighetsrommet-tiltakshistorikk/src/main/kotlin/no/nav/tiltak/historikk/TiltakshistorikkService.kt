package no.nav.tiltak.historikk

import arrow.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.tiltak.historikk.clients.Avtale
import no.nav.tiltak.historikk.clients.GraphqlRequest
import no.nav.tiltak.historikk.clients.TiltakDatadelingClient
import no.nav.tiltak.historikk.repositories.DeltakerRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TiltakshistorikkService(
    private val deltakerRepository: DeltakerRepository,
    private val tiltakDatadelingClient: TiltakDatadelingClient,
    private val cutOffDatoMapping: Map<Avtale.Tiltakstype, LocalDate>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getTiltakshistorikk(request: TiltakshistorikkRequest): TiltakshistorikkResponse = coroutineScope {
        val (identer, maxAgeYears) = request

        val arenaDeltakelser = async {
            getArenaDeltakelser(identer, maxAgeYears, cutOffDatoMapping)
        }
        val gruppetiltakDeltakelser = async {
            deltakerRepository.getKometHistorikk(identer, maxAgeYears)
        }
        val arbeidsgiverAvtaler = async {
            getArbeidsgiverAvtaler(identer, maxAgeYears, cutOffDatoMapping)
        }

        val deltakelser = arenaDeltakelser.await() + gruppetiltakDeltakelser.await()

        arbeidsgiverAvtaler
            .await()
            .fold(
                { meldinger ->
                    val historikk = deltakelser.sortedWith(compareBy(nullsLast()) { it.startDato })
                    TiltakshistorikkResponse(historikk = historikk, meldinger = meldinger)
                },
                { avtaler ->
                    val historikk = (deltakelser + avtaler).sortedWith(compareBy(nullsLast()) { it.startDato })
                    TiltakshistorikkResponse(historikk = historikk, meldinger = setOf())
                },
            )
    }

    fun getArenaDeltakelser(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
        tiltakskodeDatoMapping: Map<Avtale.Tiltakstype, LocalDate>,
    ): List<Tiltakshistorikk.ArenaDeltakelse> {
        val deltakelser = deltakerRepository.getArenaHistorikk(identer, maxAgeYears)

        return deltakelser.filter {
            val tiltakskode = arenaKodeToTeamTiltakKode(it.arenaTiltakskode) ?: return@filter true
            !belongsToTeamTiltak(tiltakskode, tiltakskodeDatoMapping, it.sluttDato)
        }
    }

    fun arenaKodeToTeamTiltakKode(arenaKode: String): Avtale.Tiltakstype? {
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

    suspend fun getArbeidsgiverAvtaler(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
        cutOffDateMap: Map<Avtale.Tiltakstype, LocalDate>,
    ): Either<NonEmptySet<TiltakshistorikkMelding>, List<Tiltakshistorikk.ArbeidsgiverAvtale>> {
        val minAvtaleDato = maxAgeYears?.let { LocalDate.now().minusYears(it.toLong()) } ?: LocalDate.MIN
        return identer
            .mapOrAccumulate {
                tiltakDatadelingClient.getAvtalerForPerson(
                    GraphqlRequest.GetAvtalerForPerson(norskIdent = it.value),
                    AccessType.M2M,
                ).bind()
            }
            .map {
                it.flatten()
                    .filter { avtale ->
                        belongsToTeamTiltak(avtale.tiltakstype, cutOffDateMap, avtale.sluttDato)
                    }
                    .filter { avtale ->
                        val avtaleDato = avtale.sluttDato
                            ?: avtale.startDato
                            ?: avtale.opprettetTidspunkt.toLocalDate()
                        !avtaleDato.isBefore(minAvtaleDato)
                    }
                    .map { avtale ->
                        toTiltakshistorikk(avtale)
                    }
            }
            .mapLeft { errors ->
                log.error("Klarte ikke hente tiltakshistorikk fra Team Tiltak. Errors=$errors")
                nonEmptySetOf(TiltakshistorikkMelding.MANGLER_HISTORIKK_FRA_TEAM_TILTAK)
            }
    }

    fun belongsToTeamTiltak(
        tiltakstype: Avtale.Tiltakstype,
        cutOffDateMap: Map<Avtale.Tiltakstype, LocalDate>,
        sluttDato: LocalDate?,
    ): Boolean {
        val cutOffDate = cutOffDateMap[tiltakstype] ?: return false
        return sluttDato == null || sluttDato.isAfter(cutOffDate) || sluttDato == cutOffDate
    }
}

private fun toTiltakshistorikk(avtale: Avtale) = Tiltakshistorikk.ArbeidsgiverAvtale(
    norskIdent = avtale.deltakerFnr,
    startDato = avtale.startDato,
    sluttDato = avtale.sluttDato,
    id = avtale.avtaleId,
    tiltakstype = when (avtale.tiltakstype) {
        Avtale.Tiltakstype.ARBEIDSTRENING -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING
        Avtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD
        Avtale.Tiltakstype.VARIG_LONNSTILSKUDD -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_LONNSTILSKUDD
        Avtale.Tiltakstype.MENTOR -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MENTOR
        Avtale.Tiltakstype.INKLUDERINGSTILSKUDD -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.INKLUDERINGSTILSKUDD
        Avtale.Tiltakstype.SOMMERJOBB -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.SOMMERJOBB
        Avtale.Tiltakstype.VTAO -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_TILRETTELAGT_ARBEID_ORDINAR
    },
    status = when (avtale.avtaleStatus) {
        Avtale.Status.ANNULLERT -> ArbeidsgiverAvtaleStatus.ANNULLERT
        Avtale.Status.AVBRUTT -> ArbeidsgiverAvtaleStatus.AVBRUTT
        Avtale.Status.PAABEGYNT -> ArbeidsgiverAvtaleStatus.PAABEGYNT
        Avtale.Status.MANGLER_GODKJENNING -> ArbeidsgiverAvtaleStatus.MANGLER_GODKJENNING
        Avtale.Status.KLAR_FOR_OPPSTART -> ArbeidsgiverAvtaleStatus.KLAR_FOR_OPPSTART
        Avtale.Status.GJENNOMFORES -> ArbeidsgiverAvtaleStatus.GJENNOMFORES
        Avtale.Status.AVSLUTTET -> ArbeidsgiverAvtaleStatus.AVSLUTTET
    },
    arbeidsgiver = Tiltakshistorikk.Arbeidsgiver(organisasjonsnummer = avtale.bedriftNr),
)
