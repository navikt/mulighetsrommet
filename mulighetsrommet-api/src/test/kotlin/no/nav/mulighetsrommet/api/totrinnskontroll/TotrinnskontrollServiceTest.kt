package no.nav.mulighetsrommet.api.totrinnskontroll

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
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
                stored.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
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
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                service.godkjent(opprettet, besluttetAv).shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                stored.status shouldBe TotrinnskontrollStatus.GODKJENT
                stored.besluttetAv shouldBe besluttetAv
                stored.besluttetTidspunkt shouldNotBe null

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TOPIC))
                records shouldHaveSize 2

                val record = records.last()
                val hendelse = Json.decodeFromString<TotrinnskontrollHendelse>(record.value.decodeToString())
                hendelse.besluttelse shouldBe TotrinnskontrollHendelse.Besluttelse.GODKJENT
                hendelse.besluttetAv shouldBe TotrinnskontrollAgent.NavAnsatt(besluttetAv.value)
                hendelse.behandletTidspunkt shouldBe stored.behandletTidspunkt
                hendelse.besluttetTidspunkt shouldBe stored.besluttetTidspunkt
            }
        }

        test("returnerer eksisterende totrinnskontroll og publiserer ny Kafka-melding") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                service.returnert(opprettet, besluttetAv, listOf("FEIL_BELOP"), "Beløp er feil").shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                stored.status shouldBe TotrinnskontrollStatus.RETURNERT
                stored.besluttetAv shouldBe besluttetAv
                stored.besluttetTidspunkt shouldNotBe null

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TOPIC))
                records shouldHaveSize 2

                val record = records.last()
                val hendelse = Json.decodeFromString<TotrinnskontrollHendelse>(record.value.decodeToString())
                hendelse.besluttelse shouldBe TotrinnskontrollHendelse.Besluttelse.AVVIST
                hendelse.besluttetAv shouldBe TotrinnskontrollAgent.NavAnsatt(besluttetAv.value)
                hendelse.aarsaker shouldBe listOf("FEIL_BELOP")
                hendelse.forklaring shouldBe "Beløp er feil"
                hendelse.behandletTidspunkt shouldBe stored.behandletTidspunkt
                hendelse.besluttetTidspunkt shouldBe stored.besluttetTidspunkt
            }
        }

        test("godkjenning feiler når behandletAv og besluttetAv er samme NavIdent") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                service.godkjent(opprettet, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Du kan ikke beslutte noe du selv har behandlet"),
                )
            }
        }

        test("retur kan gjøres av samme NavIdent som behandletAv") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                service.returnert(opprettet, behandletAv, listOf("FEIL_BELOP"), "Beløp er feil").shouldBeRight()
            }

            database.run {
                val stored = service.getOrError(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                stored.status shouldBe TotrinnskontrollStatus.RETURNERT
                stored.besluttetAv shouldBe behandletAv
            }
        }

        test("godkjenning uten årsaker-override beholder eksisterende årsaker") {
            database.run {
                val opprettet = service.opprett(
                    entityId,
                    TotrinnskontrollType.TILSAGN_ANNULLERING,
                    behandletAv,
                    aarsaker = listOf("FEIL_PERIODE"),
                )

                service.godkjent(opprettet, besluttetAv).shouldBeRight().should {
                    it.aarsaker shouldBe listOf("FEIL_PERIODE")
                }
            }
        }

        test("godkjenning feiler når totrinnskontroll allerede er godkjent") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val godkjent = service.godkjent(opprettet, besluttetAv).shouldBeRight()

                service.godkjent(godkjent, besluttetAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede godkjent"),
                )
            }
        }

        test("godkjenning er ikke tillatt etter retur") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val returnert = service.returnert(opprettet, besluttetAv, listOf("FEIL_BELOP")).shouldBeRight()

                service.godkjent(returnert, besluttetAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede returnert"),
                )
            }
        }

        test("retur feiler når totrinnskontroll allerede er godkjent") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val godkjent = service.godkjent(opprettet, besluttetAv).shouldBeRight()

                service.returnert(godkjent, besluttetAv, listOf("FEIL_BELOP")) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede godkjent"),
                )
            }
        }

        test("retur feiler når totrinnskontroll allerede er returnert") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val returnert = service.returnert(opprettet, besluttetAv, listOf("FEIL_BELOP")).shouldBeRight()

                service.returnert(returnert, besluttetAv, listOf("FEIL_BELOP")) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen er allerede returnert"),
                )
            }
        }

        test("systemet er tillatt å endre fra godkjent til avvist") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)

                val godkjent = service.godkjent(opprettet, besluttetAv).shouldBeRight()

                service.returnert(godkjent, Tiltaksadministrasjon, listOf("PROPAGERT_RETUR")).shouldBeRight()
            }
        }
    }

    context("tilbakestill") {
        test("tilbakestiller til TIL_BEHANDLING med ny behandletAv og publiserer Kafka-melding") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)

                val paVent = service.sattPaVent(opprettet, besluttetAv, forklaring = "Trenger mer info").shouldBeRight()

                val tilbakestilt = service.tilbakestill(paVent, NavIdent("DD3")).shouldBeRight()

                tilbakestilt.behandletAv shouldBe NavIdent("DD3")
                tilbakestilt.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                tilbakestilt.besluttetAv shouldBe null
                tilbakestilt.besluttetTidspunkt shouldBe null
                tilbakestilt.forklaring shouldBe null

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
                val opprettet = service.opprett(
                    entityId,
                    TotrinnskontrollType.ENKELTPLASS_OKONOMI,
                    behandletAv,
                    aarsaker = listOf("FEIL_BELOP"),
                )

                val paVent = service.sattPaVent(
                    opprettet,
                    besluttetAv,
                    aarsaker = listOf("FEIL_BELOP"),
                    forklaring = "Feil beløp",
                ).shouldBeRight()

                val tilbakestilt = service.tilbakestill(paVent, behandletAv).shouldBeRight()
                tilbakestilt.aarsaker shouldBe listOf("FEIL_BELOP")
                tilbakestilt.forklaring shouldBe null
            }
        }

        test("oppdaterer behandletTidspunkt til nåtid etter tilbakestilling") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)
                val originalTidspunkt = opprettet.behandletTidspunkt

                val paVent = service.sattPaVent(opprettet, besluttetAv).shouldBeRight()

                val tilbakestilt = service.tilbakestill(paVent, behandletAv).shouldBeRight()
                tilbakestilt.behandletTidspunkt shouldNotBe originalTidspunkt
            }
        }

        test("feiler når totrinnskontroll er TilBeslutning (besluttelse er null)") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)

                service.tilbakestill(opprettet, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen kan bare tilbakestilles når den er satt på vent"),
                )
            }
        }

        test("feiler når totrinnskontroll er GODKJENT") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)

                val godkjent = service.godkjent(opprettet, besluttetAv).shouldBeRight()

                service.tilbakestill(godkjent, behandletAv) shouldBeLeft listOf(
                    FieldError.of("Totrinnskontrollen kan bare tilbakestilles når den er satt på vent"),
                )
            }
        }

        test("feiler når totrinnskontroll er RETURNERT") {
            database.run {
                val opprettet = service.opprett(entityId, TotrinnskontrollType.ENKELTPLASS_OKONOMI, behandletAv)

                val returnert = service.returnert(opprettet, besluttetAv).shouldBeRight()

                service.tilbakestill(returnert, behandletAv) shouldBeLeft listOf(
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
