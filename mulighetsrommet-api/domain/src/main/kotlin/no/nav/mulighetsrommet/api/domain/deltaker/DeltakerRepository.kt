package no.nav.mulighetsrommet.api.domain.deltaker

import java.util.UUID

interface DeltakerRepository {
    fun save(deltaker: Deltaker)

    fun get(id: UUID): Deltaker?

    fun getByGjennomforing(gjennomforingId: UUID): List<Deltaker>

    fun delete(id: UUID)
}
