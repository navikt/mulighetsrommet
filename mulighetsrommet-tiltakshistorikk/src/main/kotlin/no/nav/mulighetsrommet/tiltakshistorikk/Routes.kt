package no.nav.mulighetsrommet.tiltakshistorikk

import arrow.core.Either
import arrow.core.mapOrAccumulate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.async
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.tiltakshistorikk.clients.Avtale
import no.nav.mulighetsrommet.tiltakshistorikk.clients.GraphqlRequest
import no.nav.mulighetsrommet.tiltakshistorikk.clients.TiltakDatadelingClient
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.LocalDate
import java.util.*

fun Route.tiltakshistorikkRoutes(
    deltakerRepository: DeltakerRepository,
    tiltakDatadelingClient: TiltakDatadelingClient,
) {
    suspend fun getArbeidsgiverAvtaler(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
    ): Either<TiltakshistorikkFeilmelding, List<Tiltakshistorikk.ArbeidsgiverAvtale>> {
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
                        Tiltakshistorikk.ArbeidsgiverAvtale(
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
                                Avtale.Status.ANNULLERT -> Tiltakshistorikk.ArbeidsgiverAvtale.Status.ANNULLERT
                                Avtale.Status.AVBRUTT -> Tiltakshistorikk.ArbeidsgiverAvtale.Status.AVBRUTT
                                Avtale.Status.PAABEGYNT -> Tiltakshistorikk.ArbeidsgiverAvtale.Status.PAABEGYNT
                                Avtale.Status.MANGLER_GODKJENNING -> Tiltakshistorikk.ArbeidsgiverAvtale.Status.MANGLER_GODKJENNING
                                Avtale.Status.KLAR_FOR_OPPSTART -> Tiltakshistorikk.ArbeidsgiverAvtale.Status.KLAR_FOR_OPPSTART
                                Avtale.Status.GJENNOMFORES -> Tiltakshistorikk.ArbeidsgiverAvtale.Status.GJENNOMFORES
                                Avtale.Status.AVSLUTTET -> Tiltakshistorikk.ArbeidsgiverAvtale.Status.AVSLUTTET
                            },
                            arbeidsgiver = Tiltakshistorikk.Arbeidsgiver(organisasjonsnummer = avtale.bedriftNr),
                        )
                    }
            }
            .mapLeft { errors ->
                application.log.error("Klarte ikke hente tiltakshistorikk fra Team Tiltak. Errors=$errors")
                TiltakshistorikkFeilmelding("Klarte ikke hente tiltak fra Team Tiltak.")
            }
    }

    authenticate {
        route("/api/v1/historikk") {
            post {
                val request = call.receive<TiltakshistorikkRequest>()

                val arenaDeltakelser = async {
                    deltakerRepository.getArenaHistorikk(request.identer, request.maxAgeYears)
                }
                val gruppetiltakDeltakelser = async {
                    deltakerRepository.getKometHistorikk(request.identer, request.maxAgeYears)
                }
                val arbeidsgiverAvtaler = async {
                    getArbeidsgiverAvtaler(request.identer, request.maxAgeYears)
                }

                val deltakelser = arenaDeltakelser.await() + gruppetiltakDeltakelser.await()

                val response: TiltakshistorikkResponse = arbeidsgiverAvtaler
                    .await()
                    .fold(
                        { feilmelding ->
                            val historikk = deltakelser.sortedWith(compareBy(nullsLast()) { it.startDato })
                            TiltakshistorikkResponse(historikk = historikk, meldinger = listOf(feilmelding))
                        },
                        { avtaler ->
                            val historikk = (deltakelser + avtaler).sortedWith(compareBy(nullsLast()) { it.startDato })
                            TiltakshistorikkResponse(historikk = historikk, meldinger = listOf())
                        },
                    )

                call.respond(response)
            }
        }

        route("/api/v1/intern/arena") {
            put("/deltaker") {
                val dbo = call.receive<ArenaDeltakerDbo>()

                deltakerRepository.upsertArenaDeltaker(dbo)

                call.respond(HttpStatusCode.OK)
            }

            delete("/deltaker/{id}") {
                val id: UUID by call.parameters

                deltakerRepository.deleteArenaDeltaker(id)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
