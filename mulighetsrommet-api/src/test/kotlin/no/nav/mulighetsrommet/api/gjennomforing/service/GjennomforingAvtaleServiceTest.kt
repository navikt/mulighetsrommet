package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sagene
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate

const val TEST_GJENNOMFORING_V2_TOPIC = "gjennomforing-v2"

class GjennomforingAvtaleServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createService(): GjennomforingAvtaleService = GjennomforingAvtaleService(
        config = GjennomforingAvtaleService.Config(TEST_GJENNOMFORING_V2_TOPIC),
        db = database.db,
        navAnsattService = mockk(relaxed = true),
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(Innlandet, Oslo, Sagene, Gjovik),
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
        ),
        avtaler = listOf(AvtaleFixtures.oppfolging),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    context("valideringsfeil ved opprettelse av gjennomføring") {
        test("får ikke opprettet gjennomføring som allerede er avsluttet") {
            val gjennomforing = GjennomforingFixtures.createGjennomforingRequest(
                AvtaleFixtures.oppfolging,
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 1, 1),
            )

            val service = createService()

            service.create(gjennomforing, bertilNavIdent, today = LocalDate.of(2023, 1, 2))
                .shouldBeLeft().shouldContainAll(
                    listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring med status Avsluttet")),
                )
        }
    }

    context("opprettelse av gjennomføring") {
        val service = createService()

        val request = GjennomforingFixtures.createGjennomforingRequest(AvtaleFixtures.oppfolging)

        test("oppretting av gjennomføring blir lagret som et utgående kafka-record") {
            service.create(request, bertilNavIdent).shouldBeRight()

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    record.key shouldBe request.id.toString().toByteArray()
                    val deserialized = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    deserialized should beInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    deserialized.id shouldBe request.id
                }
            }
        }

        test("navn på tiltak og arrangør blir tilgjengelig via fritekstsøk") {
            val gjennomforing = request.copy(detaljer = request.detaljer.copy(navn = "Veldig rart navn"))

            service.create(gjennomforing, bertilNavIdent).shouldBeRight()

            database.db.session {
                queries.gjennomforing.getAll(search = "merkelig").items.shouldBeEmpty()
                queries.gjennomforing.getAll(search = "rart").items.shouldHaveSize(1)
                queries.gjennomforing.getAll(search = ArrangorFixtures.hovedenhet.navn).items.shouldBeEmpty()
                queries.gjennomforing.getAll(search = ArrangorFixtures.underenhet1.navn).items.shouldHaveSize(1)
            }
        }

        test("løpenummer og tiltaksnummer fra Arena blir tilgjengelig via fritekstsøk") {
            val gjennomforing = service.create(request, bertilNavIdent).shouldBeRight()

            database.db.session {
                queries.gjennomforing.getAll(
                    search = "1234",
                ).items.shouldBeEmpty()
                queries.gjennomforing.getAll(
                    search = gjennomforing.lopenummer.value,
                ).items.shouldHaveSize(1)
                queries.gjennomforing.getAll(
                    search = gjennomforing.lopenummer.aar.toString(),
                ).items.shouldHaveSize(1)
                queries.gjennomforing.getAll(
                    search = gjennomforing.lopenummer.lopenummer.toString(),
                ).items.shouldHaveSize(1)
            }

            service.updateArenaData(gjennomforing.id, Gjennomforing.ArenaData(Tiltaksnummer("2025#123"), null))

            database.db.session {
                queries.gjennomforing.getAll(
                    search = "2025#123",
                ).items.shouldHaveSize(1)
                queries.gjennomforing.getAll(
                    search = "2025",
                ).items.shouldHaveSize(1)
                queries.gjennomforing.getAll(
                    search = "123",
                ).items.shouldHaveSize(1)
            }
        }

        test("navEnheter uten fylke blir filtrert vekk") {
            var gjennomforing = request.copy(
                veilederinformasjon = request.veilederinformasjon.copy(
                    navRegioner = setOf(Innlandet.enhetsnummer),
                    navKontorer = setOf(Gjovik.enhetsnummer, Sagene.enhetsnummer),
                ),
            )
            createService().create(gjennomforing, bertilNavIdent).shouldBeRight().should {
                it.kontorstruktur.shouldHaveSize(1)
                it.kontorstruktur[0].kontorer.shouldHaveSize(1)
                it.kontorstruktur[0].kontorer[0].enhetsnummer shouldBe Gjovik.enhetsnummer
                it.kontorstruktur[0].region.enhetsnummer shouldBe Innlandet.enhetsnummer
            }
        }

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            val navIdent = NavAnsattFixture.DonaldDuck.navIdent

            val gjennomforing = request.copy(detaljer = request.detaljer.copy(administratorer = setOf(navIdent)))
            service.create(gjennomforing, navIdent).shouldBeRight()

            database.assertTable("user_notification").isEmpty
        }
    }

    context("oppdatere detaljer for gjennomføring") {
        val service = createService()

        val createRequest = GjennomforingFixtures.createGjennomforingRequest(AvtaleFixtures.oppfolging)

        test("oppdaterer navn og publiserer til kafka") {
            service.create(createRequest, bertilNavIdent).shouldBeRight()

            val detaljerRequest = createRequest.detaljer.copy(navn = "Oppdatert navn")

            val updated = service.updateDetaljer(createRequest.id, detaljerRequest, bertilNavIdent).shouldBeRight()
            updated.navn shouldBe "Oppdatert navn"

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 2)
            }
        }

        test("gir feil hvis gjennomføringen ikke finnes") {
            service.updateDetaljer(createRequest.id, createRequest.detaljer.copy(navn = "Test"), bertilNavIdent)
                .shouldBeLeft().shouldContainAll(
                    listOf(FieldError.root("Gjennomføringen finnes ikke")),
                )
        }

        test("Bare nye administratorer får notifikasjon ved oppdatering") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            service.create(
                createRequest.copy(detaljer = createRequest.detaljer.copy(administratorer = setOf(identAnsatt1))),
                identAnsatt1,
            ).shouldBeRight()

            val detaljerRequest = createRequest.detaljer.copy(administratorer = setOf(identAnsatt1, identAnsatt2))

            service.updateDetaljer(createRequest.id, detaljerRequest, identAnsatt1).shouldBeRight()

            database.assertTable("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues(identAnsatt2.value)
        }
    }

    context("oppdatere veilederinformasjon for gjennomføring") {
        val service = createService()

        val createRequest = GjennomforingFixtures.createGjennomforingRequest(
            AvtaleFixtures.oppfolging,
            navRegioner = setOf(Innlandet.enhetsnummer),
            navKontorer = setOf(Gjovik.enhetsnummer),
        )

        test("oppdaterer navEnheter og beskrivelse og publiserer til kafka") {
            service.create(createRequest, bertilNavIdent).shouldBeRight()

            val veilederinfoRequest = GjennomforingVeilederinfoRequest(
                navRegioner = setOf(Innlandet.enhetsnummer),
                navKontorer = setOf(Gjovik.enhetsnummer),
                navAndreEnheter = setOf(),
                beskrivelse = "Ny beskrivelse",
                faneinnhold = null,
                kontaktpersoner = emptySet(),
            )

            service.updateVeilederinfo(createRequest.id, veilederinfoRequest, bertilNavIdent).shouldBeRight()

            database.db.session {
                val detaljer = queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(createRequest.id)
                detaljer.beskrivelse shouldBe "Ny beskrivelse"
                detaljer.kontorstruktur.shouldHaveSize(1)
            }

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 2)
            }
        }

        test("gir valideringsfeil hvis navEnhet ikke tilhører avtalen") {
            service.create(createRequest, bertilNavIdent).shouldBeRight()

            val veilederinfoRequest = GjennomforingVeilederinfoRequest(
                navRegioner = setOf(NavEnhetNummer("0000")),
                navKontorer = setOf(),
                navAndreEnheter = setOf(),
                beskrivelse = null,
                faneinnhold = null,
                kontaktpersoner = emptySet(),
            )

            service.updateVeilederinfo(createRequest.id, veilederinfoRequest, bertilNavIdent)
                .shouldBeLeft()
        }

        test("gir feil hvis gjennomføringen ikke finnes") {
            val veilederinfoRequest = GjennomforingVeilederinfoRequest(
                navRegioner = setOf(Innlandet.enhetsnummer),
                navKontorer = setOf(),
                navAndreEnheter = setOf(),
                beskrivelse = null,
                faneinnhold = null,
                kontaktpersoner = emptySet(),
            )

            service.updateVeilederinfo(createRequest.id, veilederinfoRequest, bertilNavIdent)
                .shouldBeLeft().shouldContainAll(
                    listOf(FieldError.root("Gjennomføringen finnes ikke")),
                )
        }
    }

    context("avbryte gjennomføring") {
        val service = createService()

        test("blir valideringsfeil hvis gjennomføringen ikke er aktiv") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.now(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.root("Gjennomføringen er allerede avsluttet")),
            )
        }

        test("blir valideringsfeil hvis gjennomføringen forsøkes avbrytes etter at sluttdato er passert") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.of(2023, 7, 2),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.root("Gjennomføringen er allerede avsluttet")),
            )
        }

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avbrytes") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.of(2023, 7, 1),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeRight().should {
                it.status shouldBe GjennomforingStatusType.AVBRUTT
                it.apentForPamelding shouldBe false
            }

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id).publisert.shouldBe(false)

                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    val gruppe = decoded.shouldBeInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    gruppe.id shouldBe gjennomforing.id
                    gruppe.apentForPamelding shouldBe false
                }

                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avbrutt"
                    }
            }
        }

        test("stenger gjennomføring og får status avlyst når gjennomføring avbrytes før start") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.of(2023, 6, 1),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeRight().should {
                it.status shouldBe GjennomforingStatusType.AVLYST
                it.apentForPamelding shouldBe false
            }

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id).publisert.shouldBe(false)
            }
        }
    }

    context("avslutte gjennomføring") {
        val service = createService()

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avsluttes") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
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
            ) should {
                it.status shouldBe GjennomforingStatusType.AVSLUTTET
                it.apentForPamelding shouldBe false
            }

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id).publisert.shouldBe(false)

                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    val gruppe = decoded.shouldBeInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    gruppe.id shouldBe gjennomforing.id
                    gruppe.status shouldBe GjennomforingStatusType.AVSLUTTET
                }

                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avsluttet"
                    }
            }
        }
    }

    context("tilgjengelig for arrangør") {
        test("dato for tilgjengelig for arrangør blir publisert til kafka") {
            val tilgjengeligForArrangorDato = LocalDate.now().plusDays(1)
            val startDato = LocalDate.now().plusWeeks(1)

            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(startDato = startDato)

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val service = createService()

            service.setTilgjengeligForArrangorDato(
                id = gjennomforing.id,
                tilgjengeligForArrangorDato = tilgjengeligForArrangorDato,
                navIdent = bertilNavIdent,
            ).shouldBeRight()

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    val gruppe = decoded.shouldBeInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    gruppe.tilgjengeligForArrangorFraOgMedDato shouldBe tilgjengeligForArrangorDato
                }
            }
        }
    }
})

private fun QueryContext.shouldHaveKafkaProducerRecords(topic: String, size: Int): List<StoredProducerRecord> {
    return queries.kafkaProducerRecord.getRecords(100, listOf(topic)).shouldHaveSize(size)
}
