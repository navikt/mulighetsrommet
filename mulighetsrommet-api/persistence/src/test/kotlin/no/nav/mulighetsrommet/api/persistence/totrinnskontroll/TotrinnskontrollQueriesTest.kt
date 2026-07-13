package no.nav.mulighetsrommet.api.persistence.totrinnskontroll

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.time.Instant
import java.util.UUID

class TotrinnskontrollQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    test("upsert med samme id overskriver alle felter") {
        database.runAndRollback {
            val id = UUID.randomUUID()
            val entityId = UUID.randomUUID()

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = NavIdent("B100000"),
                    behandletTidspunkt = Instant.now(),
                    status = TotrinnskontrollStatus.TIL_BEHANDLING,
                    besluttetAv = null,
                    besluttetTidspunkt = null,
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = NavIdent("B200000"),
                    behandletTidspunkt = Instant.now(),
                    status = TotrinnskontrollStatus.GODKJENT,
                    besluttetAv = Tiltaksadministrasjon,
                    besluttetTidspunkt = Instant.now(),
                    aarsaker = listOf("FEIL_BELOP"),
                    forklaring = "Feil beløp oppgitt",
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).also {
                it.id shouldBe id
                it.behandletAv shouldBe NavIdent("B200000")
                it.status shouldBe TotrinnskontrollStatus.GODKJENT
                it.besluttetAv shouldBe Tiltaksadministrasjon
                it.aarsaker shouldBe listOf("FEIL_BELOP")
                it.forklaring shouldBe "Feil beløp oppgitt"
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

    test("upsert med ulik id og samme entityId oppretter ny rad, men get returnerer bare den nyeste") {
        database.runAndRollback {
            val entityId = UUID.randomUUID()

            val gammelId = UUID.randomUUID()
            val gammeltTidspunkt = Instant.parse("2026-01-01T12:00:00Z")

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = gammelId,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = Tiltaksadministrasjon,
                    behandletTidspunkt = gammeltTidspunkt,
                    status = TotrinnskontrollStatus.RETURNERT,
                    besluttetAv = Arena,
                    besluttetTidspunkt = gammeltTidspunkt,
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            val nyId = UUID.randomUUID()
            val nyttTidspunkt = Instant.parse("2026-01-02T12:00:00Z")

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = nyId,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = NavIdent("B123456"),
                    behandletTidspunkt = nyttTidspunkt,
                    status = TotrinnskontrollStatus.TIL_BEHANDLING,
                    besluttetAv = null,
                    besluttetTidspunkt = null,
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).also {
                it.id shouldBe nyId
                it.behandletAv shouldBe NavIdent("B123456")
                it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
            }
        }
    }

    test("getDto returnerer navn for NavIdent behandletAv og besluttetAv") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navAnsatt.save(NavAnsattFixture.DonaldDuck)
            repository.navAnsatt.save(NavAnsattFixture.MikkeMus)

            val entityId = UUID.randomUUID()

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = UUID.randomUUID(),
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv = NavAnsattFixture.DonaldDuck.navIdent,
                    behandletTidspunkt = Instant.now(),
                    status = TotrinnskontrollStatus.GODKJENT,
                    besluttetAv = NavAnsattFixture.MikkeMus.navIdent,
                    besluttetTidspunkt = Instant.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )

            val dto = queries.totrinnskontroll.getDtoOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            dto.shouldBeTypeOf<TotrinnskontrollDto.Besluttet>().should {
                it.behandletAv.navn shouldBe "Donald Duck"
                it.besluttetAv.navn shouldBe "Mikke Mus"
            }
        }
    }

    test("getDto returnerer system-navn for ikke-NavIdent agenter") {
        database.runAndRollback {
            val entityId = UUID.randomUUID()

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = UUID.randomUUID(),
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

            val dto = queries.totrinnskontroll.getDtoOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            dto.shouldBeTypeOf<TotrinnskontrollDto.Besluttet>().should {
                it.behandletAv.navn shouldBe "Tiltaksadministrasjon"
                it.besluttetAv.navn shouldBe "Arena"
            }
        }
    }

    test("getDto returnerer NavIdent som fallback-navn når nav_ansatt ikke finnes") {
        database.runAndRollback {
            val entityId = UUID.randomUUID()

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = UUID.randomUUID(),
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

            val dto = queries.totrinnskontroll.getDtoOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            dto.shouldBeTypeOf<TotrinnskontrollDto.TilBeslutning>().should {
                it.behandletAv.navn shouldBe "B123456"
            }
        }
    }
})
