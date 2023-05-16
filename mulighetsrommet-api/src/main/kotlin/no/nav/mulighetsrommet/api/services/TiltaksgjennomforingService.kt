package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.ServerError
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponseError
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dto.NavEnhet
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingNotificationDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingService(
    private val tiltakstyper: TiltakstypeRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val arrangorService: ArrangorService,
    private val deltakerRepository: DeltakerRepository,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
    private val enhetService: NavEnhetService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun get(id: UUID): QueryResult<TiltaksgjennomforingAdminDto?> =
        tiltaksgjennomforingRepository.get(id)
            .map { it?.hentVirksomhetsnavnForTiltaksgjennomforing()?.hentEnhetsnavn() }

    suspend fun getAll(
        paginationParams: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): QueryResult<Pair<Int, List<TiltaksgjennomforingAdminDto>>> =
        tiltaksgjennomforingRepository
            .getAll(paginationParams, filter)
            .map { (totalCount, items) ->
                totalCount to items.map { it.hentVirksomhetsnavnForTiltaksgjennomforing().hentEnhetsnavn() }
            }

    suspend fun upsert(gjennomforing: TiltaksgjennomforingRequest): Either<StatusResponseError, TiltaksgjennomforingAdminDto> {
        return Either
            .catch { tiltakstyper.get(gjennomforing.tiltakstypeId) }
            .mapLeft {
                log.error("Klarte ikke hente tiltakstype", it)
                ServerError("Klarte ikke hente tiltakstype for id=${gjennomforing.tiltakstypeId}")
            }
            .flatMap { it?.right() ?: BadRequest("Tiltakstype mangler for id=${gjennomforing.tiltakstypeId}").left() }
            .flatMap { gjennomforing.toDbo(it) }
            .flatMap { dbo ->
                virksomhetService.syncEnhetFraBrreg(dbo.virksomhetsnummer)
                tiltaksgjennomforingRepository.upsert(dbo)
                    .flatMap { tiltaksgjennomforingRepository.get(gjennomforing.id) }
                    .mapLeft {
                        log.error("Klarte ikke lagre tiltaksgjennomføring", it.error)
                        ServerError("Klarte ikke lagre tiltaksgjennomføring")
                    }
                    .flatMap { dto -> dto?.right() ?: ServerError("Klarte ikke lagre tiltaksgjennomføring").left() }
            }
            .onRight {
                sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it)
            }
    }

    fun getNokkeltallForTiltaksgjennomforing(tiltaksgjennomforingId: UUID): TiltaksgjennomforingNokkeltallDto =
        TiltaksgjennomforingNokkeltallDto(
            antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(tiltaksgjennomforingId),
        )

    private suspend fun TiltaksgjennomforingAdminDto.hentVirksomhetsnavnForTiltaksgjennomforing(): TiltaksgjennomforingAdminDto {
        val virksomhet = this.virksomhetsnummer.let { arrangorService.hentVirksomhet(it) }
        if (virksomhet != null) {
            return this.copy(virksomhetsnavn = virksomhet.navn)
        }
        return this
    }

    private fun TiltaksgjennomforingAdminDto.hentEnhetsnavn(): TiltaksgjennomforingAdminDto {
        val enheterMedNavn: List<NavEnhet> = this.navEnheter.mapNotNull {
            val enhet = enhetService.hentEnhet(it.enhetsnummer)
            enhet?.let { it1 -> NavEnhet(enhetsnummer = it1.enhetsnummer, navn = enhet.navn) }
        }
        return this.copy(navEnheter = enheterMedNavn)
    }

    fun getAllGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforingRepository.getAllGjennomforingerSomNarmerSegSluttdato()
    }
}
