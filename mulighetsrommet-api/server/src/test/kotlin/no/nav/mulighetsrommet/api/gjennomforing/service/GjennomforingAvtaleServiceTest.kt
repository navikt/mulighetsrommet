package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
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
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

val TEST_GJENNOMFORING_V2_TOPIC = ApplicationConfigTest.kafka.topics.sisteTiltaksgjennomforingerV2Topic

class GjennomforingAvtaleServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    fun createService(): GjennomforingAvtaleService = GjennomforingAvtaleService(
        db = database.api,
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
    ) {
        val admin = setOf(NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV))
        queries.ansatt.save(NavAnsattFixture.DonaldDuck.medRoller(admin))
        queries.ansatt.save(NavAnsattFixture.MikkeMus.medRoller(admin))
    }

    beforeEach {
        domain.initialize(database.api)
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
                    record.key.decodeToString().toUUID() shouldBe request.id
                    val deserialized = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    deserialized should beInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    deserialized.id shouldBe request.id
                }
            }
        }

        test("navn på tiltak og arrangør blir tilgjengelig via fritekstsøk") {
            val gjennomforing = request.copy(detaljer = request.detaljer.copy(navn = "Veldig rart navn"))

            service.create(gjennomforing, bertilNavIdent).shouldBeRight()

            database.api.session {
                queries.gjennomforing.getAll(search = "merkelig").items.shouldBeEmpty()
                queries.gjennomforing.getAll(search = "rart").items.shouldHaveSize(1)
                queries.gjennomforing.getAll(search = ArrangorFixtures.hovedenhet.navn).items.shouldBeEmpty()
                queries.gjennomforing.getAll(search = ArrangorFixtures.underenhet1.navn).items.shouldHaveSize(1)
            }
        }

        test("løpenummer og tiltaksnummer fra Arena blir tilgjengelig via fritekstsøk") {
            val gjennomforing = service.create(request, bertilNavIdent).shouldBeRight()

            database.api.session {
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

            database.api.session {
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
                    listOf(FieldError.of("Gjennomføringen finnes ikke")),
                )
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

            database.api.session {
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
                    listOf(FieldError.of("Gjennomføringen finnes ikke")),
                )
        }
    }

    context("estimert ventetid") {
        val service = createService()

        val createRequest = GjennomforingFixtures.createGjennomforingRequest(AvtaleFixtures.oppfolging)

        test("legge til og fjerne estimert ventetid ") {
            service.create(createRequest, bertilNavIdent).shouldBeRight()

            service.setEstimertVentetid(
                createRequest.id,
                GjennomforingAvtaleDetaljer.EstimertVentetid(3, "maned"),
                bertilNavIdent,
            )

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(createRequest.id).should {
                    it.estimertVentetid shouldBe GjennomforingAvtaleDetaljer.EstimertVentetid(3, "maned")
                    service.setEstimertVentetid(createRequest.id, null, bertilNavIdent)
                }
            }

            service.setEstimertVentetid(createRequest.id, null, bertilNavIdent)

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(createRequest.id).should {
                    it.estimertVentetid.shouldBeNull()
                }
            }
        }
    }

    context("avbryte gjennomføring") {
        val service = createService()

        val feilregistrering = AarsakerOgForklaringRequest(
            aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
            forklaring = null,
        )

        test("blir valideringsfeil hvis gjennomføringen er avsluttet") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.now(),
                aarsakerOgForklaring = feilregistrering,
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.of("Gjennomføringen er allerede avsluttet")),
            )
        }

        test("blir valideringsfeil hvis gjennomføringen allerede er avbrutt") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                status = GjennomforingStatusType.AVBRUTT,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.now(),
                aarsakerOgForklaring = feilregistrering,
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.of("Gjennomføringen er allerede avbrutt")),
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
            ).initialize(database.api)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.of(2023, 7, 2),
                aarsakerOgForklaring = feilregistrering,
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.of("Gjennomføringen er allerede avsluttet")),
            )
        }

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avbrytes") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.of(2023, 7, 1),
                aarsakerOgForklaring = feilregistrering,
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

                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avbrutt"
                    }
            }
        }

        test("blir valideringsfeil hvis ny sluttdato ikke er før eksisterende sluttdato") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 8, 1),
                status = GjennomforingStatusType.GJENNOMFORES,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.of(2023, 8, 1),
                aarsakerOgForklaring = feilregistrering,
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.of("Ny sluttdato må være før gjeldende sluttdato")),
            )
        }

        test("stenger gjennomføring og får status avlyst når gjennomføring avbrytes før start") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avbrytGjennomforing(
                gjennomforing.id,
                sluttDato = LocalDate.of(2023, 6, 1),
                aarsakerOgForklaring = feilregistrering,
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

        test("blir valideringsfeil hvis gjennomføringen ikke er aktiv") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avsluttGjennomforing(gjennomforing.id, LocalDateTime.now(), bertilNavIdent).shouldBeLeft(
                listOf(FieldError.of("Gjennomføringen må være aktiv for å kunne avsluttes")),
            )
        }

        test("blir valideringsfeil hvis gjennomføringen forsøkes avsluttes før sluttdato") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                status = GjennomforingStatusType.GJENNOMFORES,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avsluttGjennomforing(
                gjennomforing.id,
                LocalDate.of(2023, 7, 1).atStartOfDay(),
                bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.of("Gjennomføringen kan ikke avsluttes før sluttdato")),
            )
        }

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avsluttes") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.avsluttGjennomforing(
                gjennomforing.id,
                LocalDate.of(2023, 7, 2).atStartOfDay(),
                bertilNavIdent,
            ).shouldBeRight().should {
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

                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avsluttet"
                    }
            }
        }
    }

    context("oppdatere Arena-data") {
        val service = createService()

        test("oppdaterer tiltaksnummer første gang og logger med riktig operasjonstekst") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            val arenaData = Gjennomforing.ArenaData(Tiltaksnummer("2025#111"), "1000")

            service.updateArenaData(gjennomforing.id, arenaData).arena shouldBe arenaData

            database.run {
                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Oppdatert med tiltaksnummer fra Arena"
                    }
            }
        }

        test("endrer eksisterende Arena-data og logger med riktig operasjonstekst") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.updateArenaData(gjennomforing.id, Gjennomforing.ArenaData(Tiltaksnummer("2025#111"), "1000"))

            val nyArenaData = Gjennomforing.ArenaData(Tiltaksnummer("2025#111"), "2000")
            service.updateArenaData(gjennomforing.id, nyArenaData).arena shouldBe nyArenaData

            database.run {
                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(2).first().should {
                        it.operation shouldBe "Endret i Arena"
                    }
            }
        }

        test("gjør ingenting og publiserer ikke til kafka når Arena-data er uendret") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            val arenaData = Gjennomforing.ArenaData(Tiltaksnummer("2025#111"), "1000")
            service.updateArenaData(gjennomforing.id, arenaData)

            service.updateArenaData(gjennomforing.id, arenaData).arena shouldBe arenaData

            database.run {
                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1)

                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1)
            }
        }
    }

    context("gjenåpne gjennomføring") {
        val service = createService()

        test("gir feil dersom gjennomføringen ikke er avsluttet") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                status = GjennomforingStatusType.GJENNOMFORES,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.gjenapneGjennomforing(
                gjennomforing.id,
                nySluttDato = LocalDate.now().plusDays(30),
                navIdent = bertilNavIdent,
            ).shouldBeLeft().shouldContainAll(
                listOf(FieldError.of("Gjennomføringen må være avsluttet for å kunne gjenåpnes")),
            )
        }

        test("gir feil dersom ny sluttdato er i fortiden") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.gjenapneGjennomforing(
                gjennomforing.id,
                nySluttDato = LocalDate.of(2024, 5, 31),
                navIdent = bertilNavIdent,
                today = LocalDate.of(2024, 6, 1),
            ).shouldBeLeft().shouldContainAll(
                listOf(FieldError.of("Ny sluttdato må være i dag eller i fremtiden")),
            )
        }

        test("gjenåpner gjennomføring, oppdaterer sluttdato, publiserer til kafka og skriver til endringshistorikken") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            val nySluttDato = LocalDate.of(2024, 12, 31)
            service.gjenapneGjennomforing(
                gjennomforing.id,
                nySluttDato = nySluttDato,
                navIdent = bertilNavIdent,
                today = LocalDate.of(2024, 6, 1),
            ).shouldBeRight().should {
                it.status shouldBe GjennomforingStatusType.GJENNOMFORES
                it.sluttDato shouldBe nySluttDato
            }

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    record.key.decodeToString().toUUID() shouldBe gjennomforing.id
                }

                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble gjenåpnet"
                    }
            }
        }
    }

    context("administratorer") {
        val service = createService()

        test("gyldig administrator blir satt") {
            val request = GjennomforingFixtures.createGjennomforingRequest(
                AvtaleFixtures.oppfolging,
                administratorer = setOf(NavAnsattFixture.DonaldDuck.navIdent),
            )

            service.create(request, bertilNavIdent).shouldBeRight()

            database.api.session {
                queries.gjennomforing.getAdministratorer(request.id).orEmpty()
                    .map { it.navIdent } shouldBe listOf(NavAnsattFixture.DonaldDuck.navIdent)
            }
        }

        test("administrator uten TILTAKSGJENNOMFORINGER_SKRIV-rolle filtreres vekk") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.FetterAnton),
            ).initialize(database.api)

            val request = GjennomforingFixtures.createGjennomforingRequest(
                AvtaleFixtures.oppfolging,
                administratorer = setOf(NavAnsattFixture.FetterAnton.navIdent),
            )

            service.create(request, bertilNavIdent).shouldBeRight()

            database.api.session {
                queries.gjennomforing.getAdministratorer(request.id).orEmpty().shouldBeEmpty()
            }
        }

        test("ingen notifikasjon hvis ny administrator er den samme som oppretter") {
            val navIdent = NavAnsattFixture.DonaldDuck.navIdent
            val request = GjennomforingFixtures.createGjennomforingRequest(
                AvtaleFixtures.oppfolging,
                administratorer = setOf(navIdent),
            )

            service.create(request, navIdent).shouldBeRight()

            database.assertTable("user_notification").isEmpty
        }

        test("bare nye administratorer får notifikasjon ved oppdatering") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent
            val request = GjennomforingFixtures.createGjennomforingRequest(
                AvtaleFixtures.oppfolging,
                administratorer = setOf(identAnsatt1),
            )

            service.create(request, identAnsatt1).shouldBeRight()

            service.updateDetaljer(
                request.id,
                request.detaljer.copy(administratorer = setOf(identAnsatt1, identAnsatt2)),
                identAnsatt1,
            ).shouldBeRight()

            database.assertTable("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues(identAnsatt2.value)
        }
    }

    context("publisering") {
        val service = createService()

        test("setPublisert oppdaterer status uten å publisere til kafka") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.setPublisert(gjennomforing.id, publisert = true, navIdent = bertilNavIdent)

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id).publisert shouldBe true

                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Tiltak publisert"
                    }

                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 0)
            }
        }

        test("setApentForPamelding oppdaterer status og publiserer til kafka") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.setApentForPamelding(gjennomforing.id, apentForPamelding = false, agent = bertilNavIdent)

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    val gruppe = decoded.shouldBeInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    gruppe.apentForPamelding shouldBe false
                }
            }
        }
    }

    context("stengt hos arrangør") {
        val service = createService()

        test("registrerer periode stengt hos arrangør og publiserer til kafka") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))

            service.setStengtHosArrangor(
                gjennomforing.id,
                periode,
                "Ferie",
                bertilNavIdent,
            ).shouldBeRight().stengt.shouldHaveSize(1).first().should {
                it.start shouldBe periode.start
                it.slutt shouldBe periode.getLastInclusiveDate()
                it.beskrivelse shouldBe "Ferie"
            }

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1)
            }
        }

        test("gir valideringsfeil hvis perioden overlapper en eksisterende periode") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            service.setStengtHosArrangor(
                gjennomforing.id,
                Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15)),
                "Ferie",
                bertilNavIdent,
            ).shouldBeRight()

            service.setStengtHosArrangor(
                gjennomforing.id,
                Periode(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20)),
                "Overlappende ferie",
                bertilNavIdent,
            ).shouldBeLeft().shouldContainExactly(
                FieldError("/periodeStart", "Perioden kan ikke overlappe med andre perioder"),
            )
        }

        test("fjerner periode stengt hos arrangør og publiserer til kafka") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
            val created =
                service.setStengtHosArrangor(gjennomforing.id, periode, "Ferie", bertilNavIdent).shouldBeRight()
            val periodeId = created.stengt.shouldHaveSize(1).first().id

            service.deleteStengtHosArrangor(gjennomforing.id, periodeId, bertilNavIdent).stengt.shouldBeEmpty()

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 2)
            }
        }
    }

    context("frikoble kontaktperson fra gjennomføring") {
        val service = createService()

        test("fjerner kontaktperson fra gjennomføringens detaljer") {
            val kontaktperson = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.underenhet1.id,
                navn = "Navn Navnesen",
                beskrivelse = null,
                telefon = null,
                epost = "navn@example.com",
                ansvarligFor = listOf(),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(
                    ArrangorFixtures.hovedenhet,
                    ArrangorFixtures.underenhet1.registrerKontaktpersoner(listOf(kontaktperson)),
                    ArrangorFixtures.underenhet2,
                ),
            ).initialize(database.api)

            val request = GjennomforingFixtures.createGjennomforingRequest(AvtaleFixtures.oppfolging).let {
                it.copy(detaljer = it.detaljer.copy(arrangorKontaktpersoner = setOf(kontaktperson.id)))
            }
            service.create(request, bertilNavIdent).shouldBeRight()

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(request.id)
                    .arrangorKontaktpersoner.shouldHaveSize(1)
            }

            service.frikobleKontaktpersonFraGjennomforing(kontaktperson.id, request.id, bertilNavIdent)

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(request.id)
                    .arrangorKontaktpersoner.shouldBeEmpty()
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
            ).initialize(database.api)

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

        test("gir valideringsfeil hvis dato ikke er satt") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(startDato = LocalDate.now().plusWeeks(1))

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            createService().setTilgjengeligForArrangorDato(
                id = gjennomforing.id,
                tilgjengeligForArrangorDato = null,
                navIdent = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError("/tilgjengeligForArrangorDato", "Dato må være satt")),
            )
        }

        test("gir valideringsfeil hvis dato er i fortiden") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(startDato = LocalDate.now().plusWeeks(1))

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            createService().setTilgjengeligForArrangorDato(
                id = gjennomforing.id,
                tilgjengeligForArrangorDato = LocalDate.now().minusDays(1),
                navIdent = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError("/tilgjengeligForArrangorDato", "Du må velge en dato som er etter dagens dato")),
            )
        }

        test("gir valideringsfeil hvis dato er etter oppstartsdato") {
            val startDato = LocalDate.now().plusWeeks(1)
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(startDato = startDato)

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            createService().setTilgjengeligForArrangorDato(
                id = gjennomforing.id,
                tilgjengeligForArrangorDato = startDato.plusDays(1),
                navIdent = bertilNavIdent,
            ).shouldBeLeft(
                listOf(
                    FieldError(
                        "/tilgjengeligForArrangorDato",
                        "Du må velge en dato som er før gjennomføringens oppstartsdato",
                    ),
                ),
            )
        }
    }
})

private fun QueryContext.shouldHaveKafkaProducerRecords(topic: String, size: Int): List<StoredProducerRecord> {
    return queries.kafkaProducerRecord.getRecords(100, listOf(topic)).shouldHaveSize(size)
}
