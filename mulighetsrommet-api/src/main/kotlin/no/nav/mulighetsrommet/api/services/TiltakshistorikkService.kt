package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerError
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserRequest
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserResponse
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkAdminDto
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val pdlClient: PdlClient,
    private val arrangorService: ArrangorService,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val tiltakstypeRepository: TiltakstypeRepository,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    // TODO Returner DeltakerKort slik at vi kan bruke korrekt visning i frontend
    suspend fun hentHistorikkForBruker(norskIdent: NorskIdent, obo: AccessType.OBO): List<TiltakshistorikkAdminDto> {
        val identer = hentHistoriskeNorskIdent(norskIdent, obo)

        val response = tiltakshistorikkClient.historikk(identer)

        val historikk = response.historikk.map {
            when (it) {
                is Tiltakshistorikk.ArenaDeltakelse -> {
                    val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(it.arenaTiltakskode)
                    TiltakshistorikkAdminDto.ArenaDeltakelse(
                        id = it.id,
                        startDato = it.startDato,
                        sluttDato = it.sluttDato,
                        status = it.status,
                        tiltakNavn = it.beskrivelse,
                        tiltakstypeNavn = tiltakstype.navn,
                        arrangor = getArrangor(it.arrangor.organisasjonsnummer),
                    )
                }

                is Tiltakshistorikk.GruppetiltakDeltakelse -> {
                    val tiltakstype = tiltakstypeRepository.getByTiltakskode(it.gjennomforing.tiltakskode)
                    TiltakshistorikkAdminDto.GruppetiltakDeltakelse(
                        id = it.id,
                        startDato = it.startDato,
                        sluttDato = it.sluttDato,
                        status = it.status,
                        tiltakNavn = it.gjennomforing.navn,
                        tiltakstypeNavn = tiltakstype.navn,
                        arrangor = getArrangor(it.arrangor.organisasjonsnummer),
                    )
                }

                is Tiltakshistorikk.ArbeidsgiverAvtale -> throw IllegalStateException("ArbeidsgiverAvtale er enda ikke støttet")
            }
        }

        // TODO Type opp noe mock-historikk i tiltakshistorikk.json for Wiremock
        // TODO Oppdater openApi med korrekt respons

        val historikkFraKometsApi =
            hentDeltakelserFraKomet(norskIdent, obo).map { it.historikk }.getOrElse { emptyList() }

        val blandetHistorikk = historikk + historikkFraKometsApi

        println(blandetHistorikk)
        return historikk // TODO Må returnere korrekt historikk
    }

    suspend fun hentDeltakelserFraKomet(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Either<AmtDeltakerError, DeltakelserResponse> {
        return amtDeltakerClient.hentDeltakelser(DeltakelserRequest(norskIdent), obo)
    }

    private suspend fun hentHistoriskeNorskIdent(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): List<NorskIdent> {
        val request = GraphqlRequest.HentHistoriskeIdenter(
            ident = PdlIdent(norskIdent.value),
            grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT),
        )
        return pdlClient.hentHistoriskeIdenter(request, obo)
            .map { identer -> identer.map { NorskIdent(it.ident.value) } }
            .getOrElse {
                when (it) {
                    PdlError.Error -> throw Exception("Feil mot pdl!")
                    PdlError.NotFound -> listOf(norskIdent)
                }
            }
    }

    private suspend fun getArrangor(orgnr: Organisasjonsnummer): TiltakshistorikkAdminDto.Arrangor {
        val navn = arrangorService.getOrSyncArrangorFromBrreg(orgnr.value).fold({ error ->
            log.warn("Klarte ikke hente arrangør. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.navn
        })

        return TiltakshistorikkAdminDto.Arrangor(organisasjonsnummer = Organisasjonsnummer(orgnr.value), navn = navn)
    }
}
