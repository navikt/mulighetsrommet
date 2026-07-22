package no.nav.mulighetsrommet.api.domain.deltaker

import java.util.UUID

interface DeltakerForslagRepository {
    fun save(forslag: DeltakerForslag)

    fun get(id: UUID): DeltakerForslag?

    fun getByGjennomforing(gjennomforingId: UUID): Map<UUID, List<DeltakerForslag>>

    fun delete(id: UUID)
}
