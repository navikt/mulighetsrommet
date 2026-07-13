package no.nav.mulighetsrommet.api.domain.arrangor

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

interface ArrangorRepository {
    fun save(arrangor: Arrangor)

    fun get(id: UUID): Arrangor

    fun getByOrganisasjonsnummer(orgnr: Organisasjonsnummer): Arrangor?

    fun delete(orgnr: Organisasjonsnummer)
}
