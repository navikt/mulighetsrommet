package no.nav.mulighetsrommet.api.domain.tiltak

import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

interface TiltakstypeRepository {
    fun getAll(tiltakskoder: Set<Tiltakskode> = emptySet()): List<Tiltakstype>
    fun get(id: UUID): Tiltakstype?
    fun getByKode(kode: Tiltakskode): Tiltakstype?
    fun upsert(tiltakstype: Tiltakstype): Tiltakstype
    fun delete(id: UUID)
}
