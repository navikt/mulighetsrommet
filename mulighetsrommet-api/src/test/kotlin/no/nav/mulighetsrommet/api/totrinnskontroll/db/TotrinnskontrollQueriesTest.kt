package no.nav.mulighetsrommet.api.totrinnskontroll.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.time.Instant
import java.util.UUID

class TotrinnskontrollQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    test("behandletAv round-trips via domain model") {
        database.runAndRollback {
            val id = UUID.randomUUID()
            val entityId = UUID.randomUUID()

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = NavIdent("B123456"),
                    behandletTidspunkt = Instant.now(),
                    status = TotrinnskontrollStatus.TIL_BEHANDLING,
                    besluttetAv = null,
                    besluttetTidspunkt = null,
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).also {
                it.behandletAv shouldBe NavIdent("B123456")
                it.besluttetAv shouldBe null
            }
        }
    }

    test("totrinnskontroll kan besluttes to ganger") {
        database.runAndRollback {
            val id = UUID.randomUUID()
            val entityId = UUID.randomUUID()

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = Tiltaksadministrasjon,
                    behandletTidspunkt = Instant.now(),
                    status = TotrinnskontrollStatus.GODKJENT,
                    besluttetAv = Tiltaksadministrasjon,
                    besluttetTidspunkt = Instant.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = Tiltaksadministrasjon,
                    behandletTidspunkt = Instant.now(),
                    status = TotrinnskontrollStatus.RETURNERT,
                    besluttetAv = Arena,
                    besluttetTidspunkt = Instant.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).should {
                it.besluttetAv shouldBe Arena
                it.status shouldBe TotrinnskontrollStatus.RETURNERT
            }
        }
    }
})
