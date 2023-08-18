package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.Tiltakskoder
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

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): AvtaleAdminDto {
        virksomhetService.getOrSyncVirksomhet(avtale.leverandorOrganisasjonsnummer)
        avtaler.upsertArenaAvtale(avtale)
        return avtaler.get(avtale.id)!!
    }

    fun removeAvtale(id: UUID) {
        avtaler.delete(id)
    }

    suspend fun upsertTiltaksgjennomforing(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingAdminDto> {
        val mulighetsrommetAvtaleId = lookForExistingAvtale(tiltaksgjennomforing)
        tiltaksgjennomforing.copy(avtaleId = mulighetsrommetAvtaleId).let {
            virksomhetService.getOrSyncVirksomhet(it.arrangorOrganisasjonsnummer)
            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(it)
        }

        val gjennomforing = tiltaksgjennomforinger.get(tiltaksgjennomforing.id)!!
        tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(gjennomforing))
        if (gjennomforing.sluttDato == null || gjennomforing.sluttDato?.isAfter(TiltaksgjennomforingSluttDatoCutoffDate) == true) {
            sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(gjennomforing)
        }
        return query { gjennomforing }
    }

    private fun lookForExistingAvtale(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo): UUID? {
        val tiltakstype = tiltakstyper.get(tiltaksgjennomforing.tiltakstypeId)
            ?: throw IllegalStateException("Ukjent tiltakstype id=${tiltaksgjennomforing.tiltakstypeId}")

        return if (Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet(tiltakstype.arenaKode)) {
            tiltaksgjennomforinger.get(tiltaksgjennomforing.id)?.avtaleId ?: tiltaksgjennomforing.avtaleId
        } else {
            tiltaksgjennomforing.avtaleId
        }
    }

    fun removeTiltaksgjennomforing(id: UUID): QueryResult<Int> {
        val i = tiltaksgjennomforinger.delete(id)
        if (i != 0) {
            tiltaksgjennomforingKafkaProducer.retract(id)
        }
        return query { i }
    }

    fun upsertTiltakshistorikk(tiltakshistorikk: ArenaTiltakshistorikkDbo): QueryResult<ArenaTiltakshistorikkDbo> {
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
