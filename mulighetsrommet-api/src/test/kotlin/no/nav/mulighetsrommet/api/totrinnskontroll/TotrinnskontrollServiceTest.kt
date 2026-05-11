package no.nav.mulighetsrommet.api.totrinnskontroll

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
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
import no.nav.mulighetsrommet.api.responses.FieldError
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

        test("støtter årsaker og forklaring") {
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

                service.godkjent(existing, besluttetAv).shouldBeRight()
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

        test("avviser med årsaker og forklaring") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)

                service.avvist(
                    existing,
                    besluttetAv,
                    aarsaker = listOf("FEIL_BELOP"),
                    forklaring = "Belopet er feil",
                ).shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe Besluttelse.AVVIST
                stored.aarsaker shouldBe listOf("FEIL_BELOP")
                stored.forklaring shouldBe "Belopet er feil"
            }
        }

        test("godkjenning feiler når behandletAv og besluttetAv er samme NavIdent") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)

                service.godkjent(existing, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Du kan ikke beslutte noe du selv har behandlet"),
                )
            }
        }

        test("avvisning kan gjøres av samme NavIdent som behandletAv") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)

                service.avvist(existing, behandletAv, listOf("FEIL_BELOP"), "Beløp er feil").shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe Besluttelse.AVVIST
                stored.besluttetAv shouldBe behandletAv
            }
        }

        test("godkjenning uten årsaker-override beholder eksisterende årsaker") {
            database.run {
                service.opprett(
                    entityId,
                    Totrinnskontroll.Type.TILSAGN_ANNULLERING,
                    behandletAv,
                    aarsaker = listOf("FEIL_PERIODE"),
                )

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_ANNULLERING)

                service.godkjent(existing, besluttetAv).shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_ANNULLERING)
                stored.aarsaker shouldBe listOf("FEIL_PERIODE")
            }
        }

        test("godkjenning feiler når totrinnskontroll allerede er godkjent") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                service.godkjent(existing, besluttetAv).shouldBeRight()

                val afterGodkjent = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                service.godkjent(afterGodkjent, besluttetAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede godkjent"),
                )
                service.avvist(afterGodkjent, besluttetAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede behandlet"),
                )
            }
        }

        test("avvisning feiler når totrinnskontroll allerede er besluttet") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)
                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                service.avvist(existing, besluttetAv, listOf("FEIL_BELOP")).shouldBeRight()

                val afterAvvist = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                service.avvist(afterAvvist, besluttetAv, listOf("FEIL_BELOP")) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede behandlet"),
                )
            }
        }

        test("godkjenning er tillatt etter avvisning") {
            database.run {
                service.opprett(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE, behandletAv)
                val existing = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                service.avvist(existing, besluttetAv, listOf("FEIL_BELOP")).shouldBeRight()

                val afterAvvist = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                service.godkjent(afterAvvist, besluttetAv).shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, Totrinnskontroll.Type.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe Besluttelse.GODKJENT
            }
        }
    }

    context("get og getOrError") {
        test("get returnerer null når totrinnskontroll ikke finnes") {
            database.run {
                service.get(UUID.randomUUID(), Totrinnskontroll.Type.TILSAGN_OPPRETTELSE).shouldBeNull()
            }
        }

        test("getOrError kaster exception når totrinnskontroll ikke finnes") {
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
