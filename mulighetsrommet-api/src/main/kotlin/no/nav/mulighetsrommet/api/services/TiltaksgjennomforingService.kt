package no.nav.mulighetsrommet.api.services

import arrow.core.flatMap
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.NavEnhet
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingNotificationDto
import java.util.*

class TiltaksgjennomforingService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val arrangorService: ArrangorService,
    private val deltakerRepository: DeltakerRepository,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
    private val enhetService: NavEnhetService,
) {

    suspend fun get(id: UUID): QueryResult<TiltaksgjennomforingAdminDto?> =
        tiltaksgjennomforingRepository.get(id)
            .map {
                it?.hentVirksomhetsnavnForTiltaksgjennomforing()?.hentVirksomhetsnavnForTiltaksgjennomforing()
                    ?.hentEnhetsnavn()
            }

    suspend fun getAll(
        paginationParams: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): QueryResult<Pair<Int, List<TiltaksgjennomforingAdminDto>>> =
        tiltaksgjennomforingRepository
            .getAll(paginationParams, filter)
            .map { (totalCount, items) ->
                totalCount to items.map { it.hentVirksomhetsnavnForTiltaksgjennomforing().hentEnhetsnavn() }
            }

    suspend fun upsert(tiltaksgjennomforingDbo: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingAdminDto> {
        virksomhetService.syncEnhetFraBrreg(tiltaksgjennomforingDbo.virksomhetsnummer)
        return tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingDbo)
            .flatMap { tiltaksgjennomforingRepository.get(tiltaksgjennomforingDbo.id) }
            .map { it!! } // If upsert is successful it should exist here
            .onRight {
                sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it)
            }
    }

    fun getNokkeltallForTiltaksgjennomforing(tiltaksgjennomforingId: UUID): TiltaksgjennomforingNokkeltallDto =
        TiltaksgjennomforingNokkeltallDto(
            antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(tiltaksgjennomforingId),
        )

    private suspend fun TiltaksgjennomforingAdminDto.hentVirksomhetsnavnForTiltaksgjennomforing(): TiltaksgjennomforingAdminDto {
        if (this.virksomhetsnavn != null) return this

        val virksomhet = this.virksomhetsnummer.let { arrangorService.hentVirksomhet(it) }
        if (virksomhet != null) {
            virksomhetService.syncEnhetFraBrreg(this.virksomhetsnummer)
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
