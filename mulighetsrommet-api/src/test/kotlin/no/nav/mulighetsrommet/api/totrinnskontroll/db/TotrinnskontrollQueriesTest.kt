package no.nav.mulighetsrommet.api.totrinnskontroll.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.time.LocalDateTime
import java.util.UUID

class TotrinnskontrollQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    test("totrinnskontroll kan besluttes to ganger") {
        database.runAndRollback {
            val id = UUID.randomUUID()
            val entityId = UUID.randomUUID()

            queries.totrinnskontroll.upsert(
                TotrinnskontrollDbo(
                    id = id,
                    entityId = entityId,
                    type = Totrinnskontroll.Type.OPPRETT,
                    behandletAv = Tiltaksadministrasjon,
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.GODKJENT,
                    besluttetAv = Tiltaksadministrasjon,
                    besluttetTidspunkt = LocalDateTime.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.upsert(
                TotrinnskontrollDbo(
                    id = id,
                    entityId = entityId,
                    type = Totrinnskontroll.Type.OPPRETT,
                    behandletAv = Tiltaksadministrasjon,
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.AVVIST,
                    besluttetAv = Arena,
                    besluttetTidspunkt = LocalDateTime.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, Totrinnskontroll.Type.OPPRETT).should {
                it.besluttetAv shouldBe Arena
                it.besluttelse shouldBe Besluttelse.AVVIST
            }
        }
    }
})
