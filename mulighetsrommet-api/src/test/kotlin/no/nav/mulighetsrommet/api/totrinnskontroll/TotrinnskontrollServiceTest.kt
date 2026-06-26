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
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.util.UUID

private const val TOPIC = "test-totrinnskontroll-topic"

class TotrinnskontrollServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

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
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
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
                payload.type shouldBe TotrinnskontrollType.TILSAGN_OPPRETTELSE
                payload.behandletAv shouldBe TotrinnskontrollAgent.NavAnsatt(behandletAv.value)
                payload.behandletTidspunkt shouldBe stored.behandletTidspunkt
                payload.besluttelse shouldBe null
                payload.besluttetTidspunkt shouldBe null
            }
        }

        test("støtter årsaker og forklaring") {
            database.run {
                service.opprett(
                    entityId,
                    TotrinnskontrollType.TILSAGN_ANNULLERING,
                    behandletAv,
                    aarsaker = listOf("FEIL_PERIODE"),
                    forklaring = "Perioden er feil",
                )
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_ANNULLERING)
                stored.aarsaker shouldBe listOf("FEIL_PERIODE")
                stored.forklaring shouldBe "Perioden er feil"
            }
        }
    }

    context("besluttet") {
        test("godkjenner eksisterende totrinnskontroll og publiserer ny Kafka-melding") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)

                service.godkjent(existing, besluttetAv).shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe TotrinnskontrollBesluttelse.GODKJENT
                stored.besluttetAv shouldBe besluttetAv
                stored.besluttetTidspunkt shouldNotBe null

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TOPIC))
                records shouldHaveSize 2

                val record = records.last()
                val hendelse = Json.decodeFromString<TotrinnskontrollHendelse>(record.value.decodeToString())
                hendelse.besluttelse shouldBe TotrinnskontrollBesluttelse.GODKJENT
                hendelse.besluttetAv shouldBe TotrinnskontrollAgent.NavAnsatt(besluttetAv.value)
                hendelse.behandletTidspunkt shouldBe stored.behandletTidspunkt
                hendelse.besluttetTidspunkt shouldBe stored.besluttetTidspunkt
            }
        }

        test("avviser eksisterende totrinnskontroll og publiserer ny Kafka-melding") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)

                service.avvist(existing, besluttetAv, listOf("FEIL_BELOP"), "Beløp er feil").shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe TotrinnskontrollBesluttelse.AVVIST
                stored.besluttetAv shouldBe besluttetAv
                stored.besluttetTidspunkt shouldNotBe null

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TOPIC))
                records shouldHaveSize 2

                val record = records.last()
                val hendelse = Json.decodeFromString<TotrinnskontrollHendelse>(record.value.decodeToString())
                hendelse.besluttelse shouldBe TotrinnskontrollBesluttelse.AVVIST
                hendelse.besluttetAv shouldBe TotrinnskontrollAgent.NavAnsatt(besluttetAv.value)
                hendelse.aarsaker shouldBe listOf("FEIL_BELOP")
                hendelse.forklaring shouldBe "Beløp er feil"
                hendelse.behandletTidspunkt shouldBe stored.behandletTidspunkt
                hendelse.besluttetTidspunkt shouldBe stored.besluttetTidspunkt
            }
        }

        test("godkjenning feiler når behandletAv og besluttetAv er samme NavIdent") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)

                service.godkjent(existing, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Du kan ikke beslutte noe du selv har behandlet"),
                )
            }
        }

        test("avvisning kan gjøres av samme NavIdent som behandletAv") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)

                service.avvist(existing, behandletAv, listOf("FEIL_BELOP"), "Beløp er feil").shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe TotrinnskontrollBesluttelse.AVVIST
                stored.besluttetAv shouldBe behandletAv
            }
        }

        test("godkjenning uten årsaker-override beholder eksisterende årsaker") {
            database.run {
                service.opprett(
                    entityId,
                    TotrinnskontrollType.TILSAGN_ANNULLERING,
                    behandletAv,
                    aarsaker = listOf("FEIL_PERIODE"),
                )

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_ANNULLERING)

                service.godkjent(existing, besluttetAv).shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_ANNULLERING)
                stored.aarsaker shouldBe listOf("FEIL_PERIODE")
            }
        }

        test("godkjenning feiler når totrinnskontroll allerede er godkjent") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.godkjent(existing, besluttetAv).shouldBeRight()

                val afterGodkjent = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.godkjent(afterGodkjent, besluttetAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede godkjent"),
                )
            }
        }

        test("godkjenning er tillatt etter avvisning") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)
                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.avvist(existing, besluttetAv, listOf("FEIL_BELOP")).shouldBeRight()

                val afterAvvist = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.godkjent(afterAvvist, besluttetAv).shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                stored.besluttelse shouldBe TotrinnskontrollBesluttelse.GODKJENT
            }
        }

        test("avvisning feiler når totrinnskontroll allerede er godkjent") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.godkjent(existing, besluttetAv).shouldBeRight()

                val afterGodkjent = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.avvist(afterGodkjent, besluttetAv, listOf("FEIL_BELOP")) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede godkjent"),
                )
            }
        }

        test("avvisning feiler når totrinnskontroll allerede er avvist") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.avvist(existing, besluttetAv, listOf("FEIL_BELOP")).shouldBeRight()

                val afterAvvist = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.avvist(afterAvvist, besluttetAv, listOf("FEIL_BELOP")) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede avvist"),
                )
            }
        }

        test("systemet er tillatt å endre fra godkjent til avvist") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val existing = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.godkjent(existing, besluttetAv).shouldBeRight()

                val afterGodkjent = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                service.avvist(afterGodkjent, Tiltaksadministrasjon, listOf("PROPAGERT_RETUR")).shouldBeRight()
            }
        }
    }

    context("tilbakestill") {
        test("tilbakestiller til TilBeslutning med ny behandletAv og publiserer Kafka-melding") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)
                val existing = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.paVent(existing, besluttetAv, forklaring = "Trenger mer info")

                val paVent = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.tilbakestill(paVent, NavIdent("DD3")).shouldBeRight()

                val stored = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                stored.behandletAv shouldBe NavIdent("DD3")
                stored.besluttelse shouldBe null
                stored.besluttetAv shouldBe null
                stored.besluttetTidspunkt shouldBe null
                stored.forklaring shouldBe null

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TOPIC))
                records shouldHaveSize 3

                val hendelse = Json.decodeFromString<TotrinnskontrollHendelse>(records.last().value.decodeToString())
                hendelse.behandletAv shouldBe TotrinnskontrollAgent.NavAnsatt("DD3")
                hendelse.besluttelse shouldBe null
                hendelse.besluttetAv shouldBe null
            }
        }

        test("beholder eksisterende årsaker etter tilbakestilling") {
            database.run {
                service.opprett(
                    entityId,
                    TotrinnskontrollType.ENKELTPLASS_OKONOMI,
                    behandletAv,
                    aarsaker = listOf("FEIL_BELOP"),
                )
                val existing = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.paVent(existing, besluttetAv, aarsaker = listOf("FEIL_BELOP"), forklaring = "Feil beløp")

                val paVent = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.tilbakestill(paVent, behandletAv).shouldBeRight()

                val stored = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                stored.aarsaker shouldBe listOf("FEIL_BELOP")
                stored.forklaring shouldBe null
            }
        }

        test("oppdaterer behandletTidspunkt til nåtid etter tilbakestilling") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)
                val existing = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                val originalTidspunkt = existing.behandletTidspunkt

                service.paVent(existing, besluttetAv)
                val paVent = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.tilbakestill(paVent, behandletAv).shouldBeRight()

                val stored = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                stored.behandletTidspunkt shouldNotBe originalTidspunkt
            }
        }

        test("feiler når totrinnskontroll er TilBeslutning (besluttelse er null)") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)
                val existing = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)

                service.tilbakestill(existing, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen kan bare tilbakestilles når den er satt på vent"),
                )
            }
        }

        test("feiler når totrinnskontroll er GODKJENT") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)
                val existing = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.godkjent(existing, besluttetAv).shouldBeRight()

                val godkjent = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.tilbakestill(godkjent, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen kan bare tilbakestilles når den er satt på vent"),
                )
            }
        }

        test("feiler når totrinnskontroll er AVVIST") {
            database.run {
                service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)
                val existing = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.avvist(existing, besluttetAv).shouldBeRight()

                val avvist = service.getOrError(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                service.tilbakestill(avvist, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen kan bare tilbakestilles når den er satt på vent"),
                )
            }
        }
    }

    context("get og getOrError") {
        test("get returnerer null når totrinnskontroll ikke finnes") {
            database.run {
                service.get(UUID.randomUUID(), TotrinnskontrollType.TILSAGN_OPPRETTELSE).shouldBeNull()
            }
        }

        test("getOrError kaster exception når totrinnskontroll ikke finnes") {
            database.run {
                shouldThrow<Exception> {
                    service.getOrError(UUID.randomUUID(), TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                }
            }
        }

        test("get returnerer lagret totrinnskontroll") {
            database.run {
                service.opprett(
                    entityId,
                    TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    behandletAv,
                )
            }

            database.run {
                service.get(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                    .shouldNotBeNull().entityId shouldBe entityId
            }
        }
    }
})
