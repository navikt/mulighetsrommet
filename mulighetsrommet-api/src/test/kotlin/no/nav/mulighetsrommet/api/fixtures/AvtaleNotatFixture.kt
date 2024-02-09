package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import java.util.*

object AvtaleNotatFixture {
    val Notat1 = AvtaleNotatDbo(
        id = UUID.randomUUID(),
        avtaleId = AvtaleFixtures.oppfolging.id,
        createdAt = null,
        updatedAt = null,
        opprettetAv = NavAnsattFixture.ansatt1.navIdent,
        innhold = "Mitt første notat",
    )
}
