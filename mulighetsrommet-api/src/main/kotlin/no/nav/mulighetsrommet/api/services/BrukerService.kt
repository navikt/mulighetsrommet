package no.nav.mulighetsrommet.api.services

import arrow.core.getOrElse
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusError
import no.nav.mulighetsrommet.api.clients.oppfolging.OppfolgingsstatusError
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.person.PersonError
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakError
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.ktor.exception.StatusException

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
    private val veilarbpersonClient: VeilarbpersonClient,
    private val navEnhetService: NavEnhetService,
) {
    suspend fun hentBrukerdata(fnr: String, accessToken: String): Brukerdata {
        val oppfolgingEnhetDto = veilarboppfolgingClient.hentOppfolgingEnhet(fnr, accessToken)
            .getOrElse {
                when (it) {
                    OppfolgingsstatusError.Forbidden -> throw StatusException(HttpStatusCode.Forbidden, "Manglet tilgang til å hente hente oppfølgingsstatus")
                    OppfolgingsstatusError.Error -> throw StatusException(HttpStatusCode.InternalServerError, "Klarte ikke hente hente oppfølgingsstatus")
                    OppfolgingsstatusError.NotFound -> null
                }
            }
        val oppfolgingStatus = veilarboppfolgingClient.hentOppfolgingStatus(fnr, accessToken)
            .getOrElse {
                when (it) {
                    OppfolgingsstatusError.Forbidden -> throw StatusException(HttpStatusCode.Forbidden, "Manglet tilgang til å hente hente oppfølgingsstatus")
                    OppfolgingsstatusError.Error -> throw StatusException(HttpStatusCode.InternalServerError, "Klarte ikke hente hente oppfølgingsstatus")
                    OppfolgingsstatusError.NotFound -> throw StatusException(HttpStatusCode.BadRequest, "Klarte ikke hente hente oppfølgingsstatus")
                }
            }
        val manuellStatus = veilarboppfolgingClient.hentManuellStatus(fnr, accessToken)
            .getOrElse {
                when (it) {
                    ManuellStatusError.Forbidden -> throw StatusException(HttpStatusCode.Forbidden, "Manglet tilgang til å hente hente manuell status")
                    ManuellStatusError.Error -> throw StatusException(HttpStatusCode.InternalServerError, "Klarte ikke hente hente manuell status")
                }
            }
        val personInfo = veilarbpersonClient.hentPersonInfo(fnr, accessToken)
            .getOrElse {
                when (it) {
                    PersonError.Forbidden -> throw StatusException(HttpStatusCode.Forbidden, "Manglet tilgang til å hente hente personinfo")
                    PersonError.Error -> throw StatusException(HttpStatusCode.InternalServerError, "Klarte ikke hente hente personinfo")
                }
            }
        val sisteVedtak = veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, accessToken)
            .getOrElse {
                when (it) {
                    VedtakError.Forbidden -> throw StatusException(HttpStatusCode.Forbidden, "Mangler tilgang til å hente §14a-vedtak")
                    VedtakError.Error -> throw StatusException(HttpStatusCode.InternalServerError, "Klarte ikke hente hente §14a-vedtak")
                    VedtakError.NotFound -> null
                }
            }

        val brukersOppfolgingsenhet = oppfolgingEnhetDto?.oppfolgingsenhet?.enhetId?.let {
            navEnhetService.hentEnhet(it)
        }

        val brukersGeografiskeEnhet = personInfo.geografiskEnhet?.enhetsnummer?.let {
            navEnhetService.hentEnhet(it)
        }

        if (sisteVedtak == null && oppfolgingEnhetDto?.servicegruppe == null) {
            throw StatusException(HttpStatusCode.BadRequest, "Bruker manglet innsatsgruppe og servicegruppe. Kontroller at brukeren er under oppfølging og finnes i Arena")
        }

        val enheter = getRelevanteEnheterForBruker(brukersGeografiskeEnhet, brukersOppfolgingsenhet)

        if (enheter.isEmpty()) {
            throw StatusException(HttpStatusCode.BadRequest, "Fant ikke brukers enheter. Kontroller at brukeren er under oppfølging og finnes i Arena")
        }

        return Brukerdata(
            fnr = fnr,
            innsatsgruppe = sisteVedtak?.innsatsgruppe,
            enheter = enheter,
            // Denne er null hvis brukeren ikke er under oppfølging
            erSykmeldtMedArbeidsgiver = oppfolgingStatus.erSykmeldtMedArbeidsgiver ?: false,
            fornavn = personInfo.fornavn,
            manuellStatus = manuellStatus,
            varsler = listOfNotNull(
                if (oppfolgingsenhetLokalOgUlik(brukersGeografiskeEnhet, brukersOppfolgingsenhet)) {
                    BrukerVarsel.LOKAL_OPPFOLGINGSENHET
                } else {
                    null
                },
            ),
        )
    }

    @Serializable
    data class Brukerdata(
        val fnr: String,
        val innsatsgruppe: Innsatsgruppe?,
        val erSykmeldtMedArbeidsgiver: Boolean,
        val enheter: List<NavEnhetDbo>,
        val fornavn: String,
        val manuellStatus: ManuellStatusDto,
        val varsler: List<BrukerVarsel>,
    )

    enum class BrukerVarsel {
        LOKAL_OPPFOLGINGSENHET,
    }
}

fun oppfolgingsenhetLokalOgUlik(
    geografiskEnhet: NavEnhetDbo?,
    oppfolgingsenhet: NavEnhetDbo?,
): Boolean {
    return oppfolgingsenhet?.type == Norg2Type.LOKAL && oppfolgingsenhet.enhetsnummer != geografiskEnhet?.enhetsnummer
}

fun getRelevanteEnheterForBruker(
    geografiskEnhet: NavEnhetDbo?,
    oppfolgingsenhet: NavEnhetDbo?,
): List<NavEnhetDbo> {
    val actualGeografiskEnhet = if (oppfolgingsenhet?.type == Norg2Type.LOKAL) {
        oppfolgingsenhet
    } else {
        geografiskEnhet
    }

    val virtuellOppfolgingsenhet = if (oppfolgingsenhet != null && oppfolgingsenhet.type !in listOf(
            Norg2Type.FYLKE,
            Norg2Type.LOKAL,
        )
    ) {
        oppfolgingsenhet
    } else {
        null
    }
    return listOfNotNull(actualGeografiskEnhet, virtuellOppfolgingsenhet)
}
