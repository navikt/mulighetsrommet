package no.nav.mulighetsrommet.api.domain.tiltakdokument

import java.util.UUID

interface TiltakDokumentRepository {
    fun save(tiltakDokument: TiltakDokument)

    fun get(id: UUID): TiltakDokument?

    fun delete(id: UUID)
}
