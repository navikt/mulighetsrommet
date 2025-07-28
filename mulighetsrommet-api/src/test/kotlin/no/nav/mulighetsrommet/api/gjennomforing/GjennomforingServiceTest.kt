package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatusDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import java.time.LocalDate
import java.time.LocalDateTime

private const val PRODUCER_TOPIC = "siste-tiltaksgjennomforinger-topic"

class GjennomforingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createService(
        validator: GjennomforingValidator = GjennomforingValidator(database.db),
    ): GjennomforingService = GjennomforingService(
        config = GjennomforingService.Config(PRODUCER_TOPIC),
        db = database.db,
        validator = validator,
        navAnsattService = mockk(relaxed = true),
    )

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    context("valideringsfeil ved opprettelse av gjennomføring") {
        test("upsert av gjennomføring dersom det oppstår valideringsfeil") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            val validator = mockk<GjennomforingValidator>()
            every { validator.validate(any(), any()) } returns listOf(
                FieldError("navn", "Dårlig navn"),
            ).left()

            val service = createService(validator)

            service.upsert(gjennomforing, bertilNavIdent).shouldBeLeft(
                listOf(FieldError("navn", "Dårlig navn")),
            )
        }

        test("får ikke opprettet gjennomføring som allerede er avsluttet") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 1, 1),
            )

            val service = createService()

            service.upsert(gjennomforing, bertilNavIdent, today = LocalDate.of(2023, 1, 2))
                .shouldBeLeft().shouldContainAll(
                    listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avsluttet")),
                )
        }
    }

    context("opprettelse av gjennomføring") {
        val validator = mockk<GjennomforingValidator>()
        every { validator.validate(any(), any()) } answers {
            firstArg<GjennomforingDbo>().right()
        }

        val service = createService(validator)

        test("oppretting av gjennomføring blir lagret som et utgående kafka-record") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            service.upsert(gjennomforing, bertilNavIdent)

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()
                record.topic shouldBe PRODUCER_TOPIC
                record.key shouldBe gjennomforing.id.toString().toByteArray()

                val decoded = Json.decodeFromString<TiltaksgjennomforingEksternV1Dto>(record.value.decodeToString())
                decoded.id shouldBe gjennomforing.id
            }
        }

        test("lagrer ikke duplikater som utgående kafka-records") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            service.upsert(gjennomforing, bertilNavIdent)
            service.upsert(gjennomforing, bertilNavIdent)

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }
        }

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            val navIdent = NavAnsattFixture.DonaldDuck.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(navIdent),
            )
            service.upsert(gjennomforing, navIdent).shouldBeRight()

            database.assertTable("user_notification").isEmpty
        }

        test("Bare nye administratorer får notifikasjon når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(identAnsatt2, identAnsatt1),
            )
            service.upsert(gjennomforing, identAnsatt1).shouldBeRight()

            database.assertTable("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues(identAnsatt2.value)
        }
    }

    context("avbryte gjennomføring") {
        val service = createService()

        test("blir valideringsfeil hvis gjennomføringen ikke er aktiv") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                status = GjennomforingStatus.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                LocalDateTime.now(),
                AvbruttAarsak.Feilregistrering,
                bertilNavIdent,
            ).shouldBeLeft(
                FieldError.root("Gjennomføringen er allerede avsluttet"),
            )
        }

        test("blir valideringsfeil hvis gjennomføringen forsøkes avbrytes etter at sluttdato er passert") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                LocalDate.of(2023, 7, 2).atStartOfDay(),
                AvbruttAarsak.Feilregistrering,
                bertilNavIdent,
            ).shouldBeLeft(
                FieldError.root("Gjennomføringen kan ikke avbrytes etter at den er avsluttet"),
            )
        }

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avbrytes") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                LocalDate.of(2023, 7, 1).atStartOfDay(),
                AvbruttAarsak.Feilregistrering,
                bertilNavIdent,
            ).shouldBeRight().should {
                it.status.shouldBeTypeOf<GjennomforingStatusDto.Avbrutt>()
                it.publisert shouldBe false
                it.apentForPamelding shouldBe false
            }

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()
                record.key shouldBe gjennomforing.id.toString().toByteArray()

                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avbrutt"
                    }
            }
        }

        test("stenger gjennomføring og får status avlyst når gjennomføring avbrytes før start") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                LocalDate.of(2023, 6, 1).atStartOfDay(),
                AvbruttAarsak.Feilregistrering,
                bertilNavIdent,
            )

            service.get(gjennomforing.id).shouldNotBeNull().should {
                it.status.shouldBeTypeOf<GjennomforingStatusDto.Avlyst>()
                it.publisert shouldBe false
                it.apentForPamelding shouldBe false
            }
        }
    }

    context("avslutte gjennomføring") {
        val service = createService()

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avsluttes") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avsluttGjennomforing(
                gjennomforing.id,
                LocalDate.of(2023, 7, 2).atStartOfDay(),
                bertilNavIdent,
            ).shouldBeRight().should {
                it.status.shouldBeTypeOf<GjennomforingStatusDto.Avsluttet>()
                it.publisert shouldBe false
                it.apentForPamelding shouldBe false
            }

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()
                record.key shouldBe gjennomforing.id.toString().toByteArray()

                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avsluttet"
                    }
            }
        }
    }

    context("tilgjengelig for arrangør") {
        test("dato for tilgjengelig for arrangør blir publisert til kafka") {
            val tilgjengeligForArrangorDato = LocalDate.of(2025, 5, 1)
            val startDato = LocalDate.of(2025, 6, 1)

            val gjennomforing = GjennomforingFixtures.AFT1.copy(startDato = startDato)

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val validator = mockk<GjennomforingValidator>()
            every {
                validator.validateTilgjengeligForArrangorDato(tilgjengeligForArrangorDato, startDato)
            } returns tilgjengeligForArrangorDato.right()

            val service = createService(validator)

            service.setTilgjengeligForArrangorDato(
                id = gjennomforing.id,
                tilgjengeligForArrangorDato = tilgjengeligForArrangorDato,
                navIdent = bertilNavIdent,
            ).shouldBeRight()

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()
                val decoded = Json.decodeFromString<TiltaksgjennomforingEksternV1Dto>(record.value.decodeToString())
                decoded.tilgjengeligForArrangorFraOgMedDato shouldBe tilgjengeligForArrangorDato
            }
        }
    }
})
