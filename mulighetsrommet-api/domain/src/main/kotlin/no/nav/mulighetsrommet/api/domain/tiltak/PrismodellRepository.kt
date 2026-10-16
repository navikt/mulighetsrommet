package no.nav.mulighetsrommet.api.domain.tiltak

import java.util.UUID

interface PrismodellRepository {
    fun upsert(prismodell: Prismodell)

    fun getOrError(id: UUID): Prismodell

    fun deletePrismodell(id: UUID)
}
