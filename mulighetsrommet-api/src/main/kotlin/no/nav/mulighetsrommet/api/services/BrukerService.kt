package no.nav.mulighetsrommet.api.services

import arrow.core.getOrElse
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.oppfolging.ErUnderOppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.OppfolgingError
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
    suspend fun hentBrukerdata(fnr: String, obo: AccessType.OBO): Brukerdata {
        val erUnderOppfolging = veilarboppfolgingClient.erBrukerUnderOppfolging(fnr, obo)
            .getOrElse {
                when (it) {
                    ErUnderOppfolgingError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Manglet tilgang til å hente oppfølgingsstatus.",
                    )

                    ErUnderOppfolgingError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente oppfølgingsstatus.",
                    )
                }
            }

        val oppfolgingsenhet = veilarboppfolgingClient.hentOppfolgingsenhet(fnr, obo)
            .getOrElse {
                when (it) {
                    OppfolgingError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Manglet tilgang til å hente oppfølgingsenhet.",
                    )

                    OppfolgingError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente oppfølgingsenhet.",
                    )

                    OppfolgingError.NotFound -> null
                }
            }
        val manuellStatus = veilarboppfolgingClient.hentManuellStatus(fnr, obo)
            .getOrElse {
                when (it) {
                    OppfolgingError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Manglet tilgang til å hente hente manuell status.",
                    )

                    OppfolgingError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente hente manuell status.",
                    )

                    OppfolgingError.NotFound -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke manuell status.",
                    )
                }
            }
        val personInfo = veilarbpersonClient.hentPersonInfo(fnr, obo)
            .getOrElse {
                when (it) {
                    PersonError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Manglet tilgang til å hente hente personinfo.",
                    )

                    PersonError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente hente personinfo.",
                    )
                }
            }
        val sisteVedtak = veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, obo)
            .getOrElse {
                when (it) {
                    VedtakError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Mangler tilgang til å hente §14a-vedtak.",
                    )

                    VedtakError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente hente §14a-vedtak.",
                    )

                    VedtakError.NotFound -> null
                }
            }

        val brukersOppfolgingsenhet = oppfolgingsenhet?.enhetId?.let {
            navEnhetService.hentEnhet(it)
        }

        val brukersGeografiskeEnhet = personInfo.geografiskEnhet?.enhetsnummer?.let {
            navEnhetService.hentEnhet(it)
        }

        val enheter = getRelevanteEnheterForBruker(brukersGeografiskeEnhet, brukersOppfolgingsenhet)

        if (enheter.isEmpty()) {
            throw StatusException(
                HttpStatusCode.BadRequest,
                "Fant ikke brukers enheter. Kontroller at brukeren er under oppfølging og finnes i Arena",
            )
        }

        return Brukerdata(
            fnr = fnr,
            innsatsgruppe = sisteVedtak?.innsatsgruppe,
            enheter = enheter,
            fornavn = personInfo.fornavn,
            manuellStatus = manuellStatus,
            varsler = buildList {
                if (oppfolgingsenhetLokalOgUlik(brukersGeografiskeEnhet, brukersOppfolgingsenhet)) {
                    add(BrukerVarsel.LOKAL_OPPFOLGINGSENHET)
                }

                if (!erUnderOppfolging && sisteVedtak?.innsatsgruppe != null) {
                    add(BrukerVarsel.BRUKER_HAR_VAERT_UNDER_OPPFOLGING)
                } else if (!erUnderOppfolging) {
                    add(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                }
            },
        )
    }

    @Serializable
    data class Brukerdata(
        val fnr: String,
        val innsatsgruppe: Innsatsgruppe?,
        val enheter: List<NavEnhetDbo>,
        val fornavn: String,
        val manuellStatus: ManuellStatusDto,
        val varsler: List<BrukerVarsel>,
    )

    enum class BrukerVarsel {
        LOKAL_OPPFOLGINGSENHET,
        BRUKER_IKKE_UNDER_OPPFOLGING,
        BRUKER_HAR_VAERT_UNDER_OPPFOLGING,
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
