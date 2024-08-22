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
import no.nav.mulighetsrommet.api.domain.dto.DeltakerKort
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkAdminDto
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.env.NaisEnv
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

    suspend fun hentHistorikkForBruker(norskIdent: NorskIdent, obo: AccessType.OBO): Map<String, List<DeltakerKort>> {
        val identer = hentHistoriskeNorskIdent(norskIdent, obo)

        val response = tiltakshistorikkClient.historikk(identer)

        val historikk: List<DeltakerKort> = response.historikk.map {
            when (it) {
                is Tiltakshistorikk.ArenaDeltakelse -> {
                    it.toDeltakerKort()
                }

                is Tiltakshistorikk.GruppetiltakDeltakelse -> {
                    it.toDeltakerKort()
                }

                is Tiltakshistorikk.ArbeidsgiverAvtale -> throw IllegalStateException("ArbeidsgiverAvtale er enda ikke støttet")
            }
        }

        val historikkFraKometsApi = hentDeltakelserFraKomet(norskIdent, obo).getOrNull()
        val kometDeltakelserFraApi = historikkFraKometsApi?.aktive?.plus(historikkFraKometsApi.historikk)
            ?: emptyList()

        val blandetHistorikk: List<DeltakerKort> = historikk.map { deltakelse ->
            val deltakelseFraKomet = kometDeltakelserFraApi.find { it.deltakerId == deltakelse.id }
            if (deltakelseFraKomet != null) {
                deltakelse.copy(
                    status = DeltakerKort.DeltakerStatus(
                        type = DeltakerKort.DeltakerStatus.DeltakerStatusType.valueOf(deltakelseFraKomet.status.type.name),
                        visningstekst = deltakelseFraKomet.status.visningstekst,
                        aarsak = deltakelseFraKomet.status.aarsak,
                    ),
                    eierskap = DeltakerKort.Eierskap.KOMET,
                )
            } else {
                deltakelse
            }
        }

        val (aktive, historiske) = blandetHistorikk.partition { erAktiv(it.status.type) }

        return mapOf(
            "aktive" to aktive,
            "historiske" to historiske,
        )
    }

    private suspend fun Tiltakshistorikk.ArenaDeltakelse.toDeltakerKort(): DeltakerKort {
        val tiltakstype = tiltakstypeRepository.getByArenaTiltakskode(arenaTiltakskode)
        return DeltakerKort(
            id = id,
            periode = DeltakerKort.Periode(
                startdato = startDato,
                sluttdato = sluttDato,
            ),
            status = DeltakerKort.DeltakerStatus(
                type = DeltakerKort.DeltakerStatus.DeltakerStatusType.valueOf(status.name),
                visningstekst = arenaStatusTilVisningstekst(status),
                aarsak = null,
            ),
            tittel = beskrivelse,
            tiltakstypeNavn = tiltakstype.navn,
            arrangorNavn = getArrangor(arrangor.organisasjonsnummer).navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakerKort.Eierskap.ARENA,
        )
    }

    private suspend fun Tiltakshistorikk.GruppetiltakDeltakelse.toDeltakerKort(): DeltakerKort {
        val tiltakstype = tiltakstypeRepository.getByTiltakskode(gjennomforing.tiltakskode)
        return DeltakerKort(
            id = id,
            periode = DeltakerKort.Periode(
                startdato = startDato,
                sluttdato = sluttDato,
            ),
            status = DeltakerKort.DeltakerStatus(
                type = DeltakerKort.DeltakerStatus.DeltakerStatusType.valueOf(status.type.name),
                visningstekst = gruppetiltakStatusTilVisningstekst(status.type),
                aarsak = gruppetiltakAarsakTilTekst(status.aarsak),
            ),
            tittel = gjennomforing.navn,
            tiltakstypeNavn = tiltakstype.navn,
            arrangorNavn = getArrangor(arrangor.organisasjonsnummer).navn,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakerKort.Eierskap.ARENA,
        )
    }

    private fun gruppetiltakAarsakTilTekst(aarsak: AmtDeltakerStatus.Aarsak?): String? {
        return when (aarsak) {
            AmtDeltakerStatus.Aarsak.SYK -> "Syk"
            AmtDeltakerStatus.Aarsak.FATT_JOBB -> "Fått jobb"
            AmtDeltakerStatus.Aarsak.TRENGER_ANNEN_STOTTE -> "Trenger annen støtte"
            AmtDeltakerStatus.Aarsak.FIKK_IKKE_PLASS -> "Fikk ikke plass"
            AmtDeltakerStatus.Aarsak.IKKE_MOTT -> "Møter ikke opp"
            AmtDeltakerStatus.Aarsak.ANNET -> "Annet"
            AmtDeltakerStatus.Aarsak.AVLYST_KONTRAKT -> "Avlyst kontrakt"
            AmtDeltakerStatus.Aarsak.UTDANNING -> "Utdanning"
            AmtDeltakerStatus.Aarsak.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT -> "Samarbeidet med arrangøren er avbrutt"
            AmtDeltakerStatus.Aarsak.FERDIG -> "Ferdig"
            AmtDeltakerStatus.Aarsak.FEILREGISTRERT -> "Feilregistrert"
            AmtDeltakerStatus.Aarsak.OPPFYLLER_IKKE_KRAVENE -> "Oppfyller ikke kravene"
            null -> null
        }
    }

    private fun erAktiv(status: DeltakerKort.DeltakerStatus.DeltakerStatusType): Boolean {
        return when (status) {
            DeltakerKort.DeltakerStatus.DeltakerStatusType.AKTUELL,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.VENTER_PA_OPPSTART,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.DELTAR,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.VURDERES,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.VENTELISTE,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.UTKAST_TIL_PAMELDING,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.SOKT_INN,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.TILBUD,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.KLADD,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.GJENNOMFORES,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.INFORMASJONSMOTE,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.TAKKET_JA_TIL_TILBUD,
            DeltakerKort.DeltakerStatus.DeltakerStatusType.PABEGYNT_REGISTRERING,
            -> true

            else -> false
        }
    }

    private fun gruppetiltakStatusTilVisningstekst(status: AmtDeltakerStatus.Type): String {
        return when (status) {
            AmtDeltakerStatus.Type.FULLFORT -> "Fullført"
            AmtDeltakerStatus.Type.VENTER_PA_OPPSTART -> "Venter på oppstart"
            AmtDeltakerStatus.Type.DELTAR -> "Deltar"
            AmtDeltakerStatus.Type.HAR_SLUTTET -> "Har sluttet"
            AmtDeltakerStatus.Type.IKKE_AKTUELL -> "Ikke aktuell"
            AmtDeltakerStatus.Type.FEILREGISTRERT -> "Feilregistrert"
            AmtDeltakerStatus.Type.PABEGYNT_REGISTRERING -> "Påbegynt registrering"
            AmtDeltakerStatus.Type.SOKT_INN -> "Søkt inn"
            AmtDeltakerStatus.Type.VURDERES -> "Vurderes"
            AmtDeltakerStatus.Type.VENTELISTE -> "Venteliste"
            AmtDeltakerStatus.Type.AVBRUTT -> "Avbrutt"
            AmtDeltakerStatus.Type.UTKAST_TIL_PAMELDING -> "Utkast til påmelding"
            AmtDeltakerStatus.Type.AVBRUTT_UTKAST -> "Avbrutt utkast"
        }
    }

    private fun arenaStatusTilVisningstekst(status: ArenaDeltakerStatus): String {
        return when (status) {
            ArenaDeltakerStatus.AVSLAG -> "Avslag"
            ArenaDeltakerStatus.IKKE_AKTUELL -> "Ikke aktuell"
            ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD -> "Takket nei til tilbud"
            ArenaDeltakerStatus.TILBUD -> "Tilbud"
            ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD -> "Takket ja til tilbud"
            ArenaDeltakerStatus.INFORMASJONSMOTE -> "Informasjonsmøte"
            ArenaDeltakerStatus.AKTUELL -> "Aktuell"
            ArenaDeltakerStatus.VENTELISTE -> "Venteliste"
            ArenaDeltakerStatus.GJENNOMFORES -> "Gjennomføres"
            ArenaDeltakerStatus.DELTAKELSE_AVBRUTT -> "Deltakelse avbrutt"
            ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT -> "Gjennomføring avbrutt"
            ArenaDeltakerStatus.GJENNOMFORING_AVLYST -> "Gjennomføring avlyst"
            ArenaDeltakerStatus.FULLFORT -> "Fullført"
            ArenaDeltakerStatus.IKKE_MOTT -> "Ikke møtt"
            ArenaDeltakerStatus.FEILREGISTRERT -> "Feilregistrert"
        }
    }

    suspend fun hentDeltakelserFraKomet(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Either<AmtDeltakerError, DeltakelserResponse> {
        // TODO Hør med Komet om vi kan hente deltakelser fra dem i Prod
        if (NaisEnv.current().isProdGCP()) {
            log.debug("Henter ikke deltakelser fra Komet sitt API i prod")
            return Either.Right(DeltakelserResponse(emptyList(), emptyList()))
        }

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
