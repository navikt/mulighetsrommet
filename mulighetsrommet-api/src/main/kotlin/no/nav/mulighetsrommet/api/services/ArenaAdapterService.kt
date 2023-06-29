package no.nav.mulighetsrommet.api.services

import arrow.core.flatMap
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import java.util.*

class ArenaAdapterService(
    private val tiltakstyper: TiltakstypeRepository,
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tiltakshistorikk: TiltakshistorikkRepository,
    private val deltakere: DeltakerRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
) {
    fun upsertTiltakstype(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> {
        return tiltakstyper.upsert(tiltakstype).onRight {
            tiltakstyper.get(tiltakstype.id)?.let {
                tiltakstypeKafkaProducer.publish(it)
            }
        }
    }

    fun removeTiltakstype(id: UUID): QueryResult<Int> {
        return tiltakstyper.delete(id).onRight { deletedRows ->
            if (deletedRows != 0) {
                tiltakstypeKafkaProducer.retract(id)
            }
        }
    }

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): QueryResult<AvtaleAdminDto> {
        virksomhetService.hentEnhet(avtale.leverandorOrganisasjonsnummer)
        return avtaler.upsertArenaAvtale(avtale)
            .flatMap { avtaler.get(avtale.id) }
            .map { it!! }
    }

    fun removeAvtale(id: UUID): QueryResult<Int> {
        return avtaler.delete(id)
    }

    suspend fun upsertTiltaksgjennomforing(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingAdminDto> {
        virksomhetService.hentEnhet(tiltaksgjennomforing.arrangorOrganisasjonsnummer)
        return tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(tiltaksgjennomforing)
            .flatMap { tiltaksgjennomforinger.get(tiltaksgjennomforing.id) }
            .map { it!! }
            .onRight {
                tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(it))
            }
            .onRight {
                if (it.sluttDato == null || it.sluttDato?.isAfter(TiltaksgjennomforingSluttDatoCutoffDate) == true) {
                    sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it)
                }
            }
    }

    fun removeTiltaksgjennomforing(id: UUID): QueryResult<Int> {
        return tiltaksgjennomforinger.delete(id)
            .onRight { deletedRows ->
                if (deletedRows != 0) {
                    tiltaksgjennomforingKafkaProducer.retract(id)
                }
            }
    }

    fun upsertTiltakshistorikk(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<TiltakshistorikkDbo> {
        return this.tiltakshistorikk.upsert(tiltakshistorikk)
    }

    fun removeTiltakshistorikk(id: UUID): QueryResult<Unit> {
        return tiltakshistorikk.delete(id)
    }

    fun upsertDeltaker(deltaker: DeltakerDbo): QueryResult<DeltakerDbo> {
        return query { deltakere.upsert(deltaker) }
    }

    fun removeDeltaker(id: UUID): QueryResult<Unit> {
        return query { deltakere.delete(id) }
    }
}
