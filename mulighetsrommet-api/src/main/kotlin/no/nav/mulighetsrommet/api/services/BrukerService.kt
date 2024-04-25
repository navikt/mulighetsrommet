package no.nav.mulighetsrommet.api.services

import arrow.core.getOrElse
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.oppfolging.ErUnderOppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.OppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakDto
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakError
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.ktor.exception.StatusException

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
    private val navEnhetService: NavEnhetService,
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client,
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
        val pdlPerson = pdlClient.hentPerson(fnr, obo)
            .getOrElse {
                when (it) {
                    PdlError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Feil ved henting av personinfo fra Pdl.",
                    )

                    PdlError.NotFound -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke person i Pdl.",
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
                        "Klarte ikke hente §14a-vedtak.",
                    )

                    VedtakError.NotFound -> null
                }
            }

        val brukersOppfolgingsenhet = oppfolgingsenhet?.enhetId?.let {
            navEnhetService.hentEnhet(it)
        }

        val geografiskTilknytning = pdlClient.hentGeografiskTilknytning(fnr, obo)
            .getOrElse {
                when (it) {
                    PdlError.Error -> {
                        throw StatusException(
                            HttpStatusCode.InternalServerError,
                            "Klarte ikke hente geografisk tilknytning fra Pdl.",
                        )
                    }

                    PdlError.NotFound -> null
                }
            }

        val brukersGeografiskeEnhet = geografiskTilknytning?.let { hentBrukersGeografiskeEnhet(it) }

        val enheter = getRelevanteEnheterForBruker(brukersGeografiskeEnhet, brukersOppfolgingsenhet)

        if (enheter.isEmpty()) {
            throw StatusException(
                HttpStatusCode.BadRequest,
                "Fant ikke brukers enheter. Kontroller at brukeren er under oppfølging og finnes i Arena",
            )
        }

        return Brukerdata(
            fnr = fnr,
            innsatsgruppe = sisteVedtak?.innsatsgruppe?.let { toInnsatsgruppe(it) },
            enheter = enheter,
            fornavn = pdlPerson.navn.firstOrNull()?.fornavn,
            manuellStatus = manuellStatus,
            erUnderOppfolging = erUnderOppfolging,
            varsler = buildList {
                if (oppfolgingsenhetLokalOgUlik(brukersGeografiskeEnhet, brukersOppfolgingsenhet)) {
                    add(BrukerVarsel.LOKAL_OPPFOLGINGSENHET)
                }

                if (!erUnderOppfolging && sisteVedtak?.innsatsgruppe != null) {
                    add(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                } else if (!erUnderOppfolging) {
                    add(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                } else if (sisteVedtak?.innsatsgruppe == null) {
                    add(BrukerVarsel.BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK)
                }
            },
        )
    }

    private suspend fun hentBrukersGeografiskeEnhet(geografiskTilknytning: GeografiskTilknytning): NavEnhetDbo? {
        val norgResult = when (geografiskTilknytning) {
            is GeografiskTilknytning.GtBydel -> norg2Client.hentEnhetByGeografiskOmraade(geografiskTilknytning.value)
            is GeografiskTilknytning.GtKommune -> norg2Client.hentEnhetByGeografiskOmraade(geografiskTilknytning.value)
            is GeografiskTilknytning.GtUtland, GeografiskTilknytning.GtUdefinert -> null
        } ?: return null

        return norgResult
            .map { navEnhetService.hentEnhet(it.enhetNr) }
            .getOrElse {
                when (it) {
                    NorgError.NotFound -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke nav enhet til geografisk tilknytning.",
                    )

                    NorgError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke nav enhet til geografisk tilknytning.",
                    )
                }
            }
    }

    @Serializable
    data class Brukerdata(
        val fnr: String,
        val innsatsgruppe: Innsatsgruppe?,
        val enheter: List<NavEnhetDbo>,
        val fornavn: String?,
        val manuellStatus: ManuellStatusDto,
        val erUnderOppfolging: Boolean,
        val varsler: List<BrukerVarsel>,
    )

    enum class BrukerVarsel {
        LOKAL_OPPFOLGINGSENHET,
        BRUKER_IKKE_UNDER_OPPFOLGING,
        BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK,
    }
}

private fun toInnsatsgruppe(innsatsgruppe: VedtakDto.Innsatsgruppe): Innsatsgruppe {
    return when (innsatsgruppe) {
        VedtakDto.Innsatsgruppe.STANDARD_INNSATS -> Innsatsgruppe.STANDARD_INNSATS
        VedtakDto.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
        VedtakDto.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS -> Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
        // TODO: benytt verdi for GRADERT_VARIG_TILPASSET_INNSATS når ny 14a-løsning er lansert nasjonalt
        VedtakDto.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS, VedtakDto.Innsatsgruppe.VARIG_TILPASSET_INNSATS -> Innsatsgruppe.VARIG_TILPASSET_INNSATS
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
