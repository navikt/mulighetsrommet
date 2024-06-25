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
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val pdlClient: PdlClient,
    private val arrangorService: ArrangorService,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val tiltakshistorikkRepository: TiltakshistorikkRepository,
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val tiltakstyper: TiltakstypeRepository,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentHistorikkForBrukerV2(norskIdent: NorskIdent, obo: AccessType.OBO): List<TiltakshistorikkAdminDto> {
        val identer = hentHistoriskeNorskIdent(norskIdent, obo)

        val response = tiltakshistorikkClient.historikk(identer)

        return response.historikk.map {
            when (it) {
                is Tiltakshistorikk.ArenaDeltakelse -> {
                    val tiltakstype = tiltakstyper.getByArenaTiltakskode(it.arenaTiltakskode)
                    TiltakshistorikkAdminDto(
                        id = it.id,
                        fraDato = it.startDato,
                        tilDato = it.sluttDato,
                        status = toDeltakerstatus(it.status),
                        tiltaksnavn = it.beskrivelse,
                        tiltakstype = tiltakstype.navn,
                        arrangor = getArrangor(it.arrangor.organisasjonsnummer),
                    )
                }

                is Tiltakshistorikk.GruppetiltakDeltakelse -> {
                    val tiltakstype = tiltakstyper.getByTiltakskode(it.gjennomforing.tiltakskode)
                    TiltakshistorikkAdminDto(
                        id = it.id,
                        fraDato = it.startDato,
                        tilDato = it.sluttDato,
                        status = toDeltakerstatus(it.status),
                        tiltaksnavn = it.gjennomforing.navn,
                        tiltakstype = tiltakstype.navn,
                        arrangor = getArrangor(it.arrangor.organisasjonsnummer),
                    )
                }

                is Tiltakshistorikk.ArbeidsgiverAvtale -> throw IllegalStateException("ArbeidsgiverAvtale er enda ikke støttet")
            }
        }
    }

    suspend fun hentHistorikkForBruker(norskIdent: NorskIdent, obo: AccessType.OBO): List<TiltakshistorikkAdminDto> {
        val identer = hentHistoriskeNorskIdent(norskIdent, obo)

        return tiltakshistorikkRepository.getTiltakshistorikkForBruker(identer).map { deltakelse ->
            val arrangor = deltakelse.arrangorOrganisasjonsnummer?.let { orgnr ->
                getArrangor(Organisasjonsnummer(orgnr))
            }

            deltakelse.run {
                TiltakshistorikkAdminDto(
                    id = id,
                    fraDato = fraDato,
                    tilDato = tilDato,
                    status = status,
                    tiltaksnavn = tiltaksnavn,
                    tiltakstype = tiltakstype,
                    arrangor = arrangor,
                )
            }
        }
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

fun toDeltakerstatus(status: ArenaDeltakerStatus) =
    when (status) {
        ArenaDeltakerStatus.AVSLAG,
        ArenaDeltakerStatus.IKKE_AKTUELL,
        ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD,
        -> Deltakerstatus.IKKE_AKTUELL

        ArenaDeltakerStatus.TILBUD,
        ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD,
        ArenaDeltakerStatus.INFORMASJONSMOTE,
        ArenaDeltakerStatus.AKTUELL,
        ArenaDeltakerStatus.VENTELISTE,
        -> Deltakerstatus.VENTER

        ArenaDeltakerStatus.GJENNOMFORES -> Deltakerstatus.DELTAR
        ArenaDeltakerStatus.DELTAKELSE_AVBRUTT,
        ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT,
        ArenaDeltakerStatus.GJENNOMFORING_AVLYST,
        ArenaDeltakerStatus.FULLFORT,
        ArenaDeltakerStatus.IKKE_MOTT,
        -> Deltakerstatus.AVSLUTTET
    }

fun toDeltakerstatus(status: AmtDeltakerStatus) =
    when (status.type) {
        AmtDeltakerStatus.Type.PABEGYNT_REGISTRERING -> Deltakerstatus.PABEGYNT_REGISTRERING

        AmtDeltakerStatus.Type.UTKAST_TIL_PAMELDING, // TODO: Skal denne her? Dette er vel før påbegynt registrering egentlig?
        AmtDeltakerStatus.Type.VURDERES, // TODO: Skal denne her? Dette er vel før påbegynt registrering egentlig?
        AmtDeltakerStatus.Type.SOKT_INN,
        AmtDeltakerStatus.Type.VENTELISTE,
        AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
        -> Deltakerstatus.VENTER

        AmtDeltakerStatus.Type.AVBRUTT_UTKAST,
        AmtDeltakerStatus.Type.IKKE_AKTUELL,
        AmtDeltakerStatus.Type.FEILREGISTRERT,
        -> Deltakerstatus.IKKE_AKTUELL

        AmtDeltakerStatus.Type.DELTAR -> Deltakerstatus.DELTAR

        AmtDeltakerStatus.Type.HAR_SLUTTET,
        AmtDeltakerStatus.Type.AVBRUTT,
        AmtDeltakerStatus.Type.FULLFORT,
        -> Deltakerstatus.AVSLUTTET
    }
