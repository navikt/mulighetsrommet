package no.nav.mulighetsrommet.api.persistence.totrinnskontroll

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
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
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = NavIdent("B100000"),
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    beslutning = null,
                    status = TotrinnskontrollStatus.TIL_BEHANDLING,
                ),
            )

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = NavIdent("B200000"),
                        tidspunkt = Instant.now(),
                        begrunnelse = "Begrunnelse fra behandler",
                        aarsaker = listOf("BEHANDLET_AARSAK"),
                    ),
                    beslutning = Totrinnskontroll.Beslutning(
                        utfortAv = Tiltaksadministrasjon,
                        tidspunkt = Instant.now(),
                        begrunnelse = "Feil beløp oppgitt",
                        aarsaker = listOf("FEIL_BELOP"),
                    ),
                    status = TotrinnskontrollStatus.GODKJENT,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).also {
                it.id shouldBe id
                it.behandling.utfortAv shouldBe NavIdent("B200000")
                it.status shouldBe TotrinnskontrollStatus.GODKJENT
                it.beslutning?.utfortAv shouldBe Tiltaksadministrasjon
                it.behandling.begrunnelse shouldBe "Begrunnelse fra behandler"
                it.behandling.aarsaker shouldBe listOf("BEHANDLET_AARSAK")
                it.beslutning?.begrunnelse shouldBe "Feil beløp oppgitt"
                it.beslutning?.aarsaker shouldBe listOf("FEIL_BELOP")
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
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = Tiltaksadministrasjon,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    beslutning = Totrinnskontroll.Beslutning(
                        utfortAv = Tiltaksadministrasjon,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    status = TotrinnskontrollStatus.GODKJENT,
                ),
            )

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = Tiltaksadministrasjon,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    beslutning = Totrinnskontroll.Beslutning(
                        utfortAv = Arena,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    status = TotrinnskontrollStatus.RETURNERT,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).should {
                it.beslutning?.utfortAv shouldBe Arena
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
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = Tiltaksadministrasjon,
                        tidspunkt = gammeltTidspunkt,
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    beslutning = Totrinnskontroll.Beslutning(
                        utfortAv = Arena,
                        tidspunkt = gammeltTidspunkt,
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    status = TotrinnskontrollStatus.RETURNERT,
                ),
            )

            val nyId = UUID.randomUUID()
            val nyttTidspunkt = Instant.parse("2026-01-02T12:00:00Z")

            queries.totrinnskontroll.upsert(
                Totrinnskontroll(
                    id = nyId,
                    entityId = entityId,
                    type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = NavIdent("B123456"),
                        tidspunkt = nyttTidspunkt,
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    beslutning = null,
                    status = TotrinnskontrollStatus.TIL_BEHANDLING,
                ),
            )

            queries.totrinnskontroll.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE).also {
                it.id shouldBe nyId
                it.behandling.utfortAv shouldBe NavIdent("B123456")
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
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = NavAnsattFixture.DonaldDuck.navIdent,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    beslutning = Totrinnskontroll.Beslutning(
                        utfortAv = NavAnsattFixture.MikkeMus.navIdent,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    status = TotrinnskontrollStatus.GODKJENT,
                ),
            )

            val dto = queries.totrinnskontroll.getDtoOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            dto.shouldBeTypeOf<TotrinnskontrollDto.Besluttet>().should {
                it.behandling.utfortAv.navn shouldBe "Donald Duck"
                it.beslutning.utfortAv.navn shouldBe "Mikke Mus"
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
                    behandling = Totrinnskontroll.Behandling(
                        utfortAv = Tiltaksadministrasjon,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    beslutning = Totrinnskontroll.Beslutning(
                        utfortAv = Arena,
                        tidspunkt = Instant.now(),
                        begrunnelse = null,
                        aarsaker = emptyList(),
                    ),
                    status = TotrinnskontrollStatus.RETURNERT,
                ),
            )

            val dto = queries.totrinnskontroll.getDtoOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            dto.shouldBeTypeOf<TotrinnskontrollDto.Besluttet>().should {
                it.behandling.utfortAv.navn shouldBe "Tiltaksadministrasjon"
                it.beslutning.utfortAv.navn shouldBe "Arena"
            }
        }
    }
})
