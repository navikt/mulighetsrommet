package no.nav.mulighetsrommet.api.totrinnskontroll

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

private const val TOPIC = "test-totrinnskontroll-topic"

class TotrinnskontrollServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val service = TotrinnskontrollService(TOPIC)

    beforeEach {
        MulighetsrommetTestDomain().initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val behandletAv = NavIdent("DD1")
    val besluttetAv = NavIdent("DD2")
    val entityId = UUID.randomUUID()

    context("opprett") {
        test("lagrer totrinnskontroll med blank besluttelse og publiserer Kafka-melding") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                stored.entityId shouldBe entityId
                stored.behandletAv shouldBe behandletAv
                stored.besluttelse shouldBe null
                stored.besluttetAv shouldBe null

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TOPIC))
                records shouldHaveSize 1

                val record = records.first()
                record.key.decodeToString() shouldBe entityId.toString()
                val payload = Json.decodeFromString<TotrinnskontrollHendelse>(record.value.decodeToString())
                payload.entityId shouldBe entityId
                payload.type shouldBe Totrinnskontroll.Type.TILSAGN_OPPRETTELSE
                payload.behandletAv shouldBe behandletAv
                payload.besluttelse shouldBe null
            }
        }

        test("støtter aarsaker og forklaring") {
            database.run {
                service.opprett(
                    entityId,
                    Totrinnskontroll.Type.TILSAGN_ANNULLERING,
                    behandletAv,
                    aarsaker = listOf("FEIL_PERIODE"),
                    forklaring = "Perioden er feil",
                )
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_ANNULLERING)
                stored.aarsaker shouldBe listOf("FEIL_PERIODE")
                stored.forklaring shouldBe "Perioden er feil"
            }
        }
    }

    context("besluttet") {
        test("godkjenner eksisterende totrinnskontroll og publiserer ny Kafka-melding") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)

                service.besluttet(
                    existing,
                    besluttetAv,
                    Besluttelse.GODKJENT,
                )
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe Besluttelse.GODKJENT
                stored.besluttetAv shouldBe besluttetAv
                stored.besluttetTidspunkt shouldNotBe null

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TOPIC))
                records shouldHaveSize 2

                val record = records.last()
                val hendelse = Json.decodeFromString<TotrinnskontrollHendelse>(record.value.decodeToString())
                hendelse.besluttelse shouldBe Besluttelse.GODKJENT
                hendelse.besluttetAv shouldBe besluttetAv
            }
        }

        test("avviser med aarsaker og forklaring") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)

                service.besluttet(
                    existing,
                    besluttetAv,
                    Besluttelse.AVVIST,
                    aarsaker = listOf("FEIL_BELOP"),
                    forklaring = "Belopet er feil",
                )
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe Besluttelse.AVVIST
                stored.aarsaker shouldBe listOf("FEIL_BELOP")
                stored.forklaring shouldBe "Belopet er feil"
            }
        }

        test("godkjenning uten aarsaker-override beholder eksisterende aarsaker") {
            database.run {
                service.opprett(
                    entityId,
                    Totrinnskontroll.Type.TILSAGN_ANNULLERING,
                    behandletAv,
                    aarsaker = listOf("FEIL_PERIODE"),
                )

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_ANNULLERING)

                service.besluttet(
                    existing,
                    besluttetAv,
                    Besluttelse.GODKJENT,
                )
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_ANNULLERING)
                stored.aarsaker shouldBe listOf("FEIL_PERIODE")
            }
        }
    }

    context("get og getOrError") {
        test("get returnerer null når totrinnskontroll ikke finnes") {
            database.run {
                service.get(UUID.randomUUID(), Totrinnskontroll.Type.TILSAGN_OPPRETTELSE).shouldBeNull()
            }
        }

        test("getOrError kaster exception naar totrinnskontroll ikke finnes") {
            database.run {
                shouldThrow<Exception> {
                    service.getOrError(UUID.randomUUID(), Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                }
            }
        }

        test("get returnerer lagret totrinnskontroll") {
            database.run {
                service.opprett(
                    entityId,
                    Totrinnskontroll.Type.TILSAGN_OPPRETTELSE,
                    behandletAv,
                )
            }

            database.run {
                service.get(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                    .shouldNotBeNull().entityId shouldBe entityId
            }
        }
    }
})
