package no.nav.mulighetsrommet.api.domain.navansatt

import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

interface NavAnsattRepository {
    fun save(ansatt: NavAnsatt)

    fun get(navIdent: NavIdent): NavAnsatt?

    fun getOrError(navIdent: NavIdent): NavAnsatt

    fun getByEntraObjectId(objectId: UUID): NavAnsatt?

    fun getAll(): List<NavAnsatt>

    fun deleteByEntraObjectId(objectId: UUID): Int
}
