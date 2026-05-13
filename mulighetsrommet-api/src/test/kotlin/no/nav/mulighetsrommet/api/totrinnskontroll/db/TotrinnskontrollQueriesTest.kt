package no.nav.mulighetsrommet.api.totrinnskontroll.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.time.Instant
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
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = Tiltaksadministrasjon,
                    behandletTidspunkt = Instant.now(),
                    besluttelse = TotrinnskontrollBesluttelse.GODKJENT,
                    besluttetAv = Tiltaksadministrasjon,
                    besluttetTidspunkt = Instant.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.upsert(
                TotrinnskontrollDbo(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = Tiltaksadministrasjon,
                    behandletTidspunkt = Instant.now(),
                    besluttelse = TotrinnskontrollBesluttelse.AVVIST,
                    besluttetAv = Arena,
                    besluttetTidspunkt = Instant.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).should {
                it.besluttetAv shouldBe Arena
                it.besluttelse shouldBe TotrinnskontrollBesluttelse.AVVIST
            }
        }
    }
})
