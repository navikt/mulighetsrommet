package no.nav.mulighetsrommet.tiltakshistorikk

import arrow.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.tiltakshistorikk.clients.Avtale
import no.nav.mulighetsrommet.tiltakshistorikk.clients.GraphqlRequest
import no.nav.mulighetsrommet.tiltakshistorikk.clients.TiltakDatadelingClient
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TiltakshistorikkService(
    private val deltakerRepository: DeltakerRepository,
    private val tiltakDatadelingClient: TiltakDatadelingClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getTiltakshistorikk(request: TiltakshistorikkRequest): TiltakshistorikkResponse = coroutineScope {
        val (identer, maxAgeYears) = request
        val arenaDeltakelser = async {
            deltakerRepository.getArenaHistorikk(identer, maxAgeYears)
        }
        val gruppetiltakDeltakelser = async {
            deltakerRepository.getKometHistorikk(identer, maxAgeYears)
        }
        val arbeidsgiverAvtaler = async {
            getArbeidsgiverAvtaler(identer, maxAgeYears)
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

    private suspend fun getArbeidsgiverAvtaler(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
    ): Either<NonEmptySet<TiltakshistorikkMelding>, List<Tiltakshistorikk.ArbeidsgiverAvtale>> {
        if (NaisEnv.current().isProdGCP()) {
            return nonEmptySetOf(TiltakshistorikkMelding.HENTER_IKKE_HISTORIKK_FRA_TEAM_TILTAK).left()
        }

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
                        val avtaleDato = avtale.sluttDato
                            ?: avtale.startDato
                            ?: avtale.registrertTidspunkt.toLocalDate()
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
}

private fun toTiltakshistorikk(avtale: Avtale) = Tiltakshistorikk.ArbeidsgiverAvtale(
    norskIdent = avtale.deltakerFnr,
    startDato = avtale.startDato,
    sluttDato = avtale.sluttDato,
    avtaleId = avtale.avtaleId,
    tiltakstype = when (avtale.tiltakstype) {
        Avtale.Tiltakstype.ARBEIDSTRENING -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING
        Avtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD
        Avtale.Tiltakstype.VARIG_LONNSTILSKUDD -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.VARIG_LONNSTILSKUDD
        Avtale.Tiltakstype.MENTOR -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.MENTOR
        Avtale.Tiltakstype.INKLUDERINGSTILSKUDD -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.INKLUDERINGSTILSKUDD
        Avtale.Tiltakstype.SOMMERJOBB -> Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.SOMMERJOBB
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
