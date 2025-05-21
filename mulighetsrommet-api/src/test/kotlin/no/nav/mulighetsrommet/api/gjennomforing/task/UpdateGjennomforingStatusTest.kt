package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.GjennomforingStatus.*
import no.nav.mulighetsrommet.model.GjennomforingStatusDto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import java.time.LocalDate
import java.util.*

private const val PRODUCER_TOPIC = "siste-tiltaksgjennomforinger-topic"

class UpdateGjennomforingStatusTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createTask() = UpdateGjennomforingStatus(
        database.db,
        GjennomforingService(
            config = GjennomforingService.Config(PRODUCER_TOPIC),
            db = database.db,
            validator = mockk(relaxed = true),
            navAnsattService = mockk(relaxed = true),
        ),
    )

    context("oppdater statuser på tiltaksgjennomføringer") {
        val gjennomforing1 = GjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 12, 31),
            status = GJENNOMFORES,
        )
        val gjennomforing2 = GjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 1, 31),
            status = GJENNOMFORES,
        )
        val gjennomforing3 = GjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 1, 31),
            status = GJENNOMFORES,
        )
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(
                gjennomforing1,
                gjennomforing2,
                gjennomforing3,
            ),
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("forsøker ikke å avslutte gjennomføringer før sluttDato er passert") {
            val task = createTask()

            task.oppdaterGjennomforingStatus(today = LocalDate.of(2023, 1, 31))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
                }

                queries.kafkaProducerRecord.getRecords(10).shouldBeEmpty()
            }
        }

        test("avslutter gjennomføringer når sluttDato er passert") {
            val task = createTask()

            task.oppdaterGjennomforingStatus(today = LocalDate.of(2023, 2, 1))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(AVSLUTTET, avbrutt = null))
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(AVSLUTTET, avbrutt = null))
                }

                queries.kafkaProducerRecord.getRecords(10).also { records ->
                    records.shouldHaveSize(2)
                    records.forEach {
                        it.topic shouldBe PRODUCER_TOPIC
                    }

                    Json.decodeFromString<TiltaksgjennomforingEksternV1Dto>(records[0].value.decodeToString()).should {
                        it.id shouldBe gjennomforing2.id
                        it.status shouldBe AVSLUTTET
                    }

                    Json.decodeFromString<TiltaksgjennomforingEksternV1Dto>(records[1].value.decodeToString()).should {
                        it.id shouldBe gjennomforing3.id
                        it.status shouldBe AVSLUTTET
                    }
                }
            }
        }

        test("avslutter gjennomføringer når sluttDato er passert (sluttDato passert med flere dager)") {
            val task = createTask()

            task.oppdaterGjennomforingStatus(today = LocalDate.of(2023, 3, 1))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(AVSLUTTET, avbrutt = null))
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(AVSLUTTET, avbrutt = null))
                }

                queries.kafkaProducerRecord.getRecords(10).also { records ->
                    records.shouldHaveSize(2)

                    Json.decodeFromString<TiltaksgjennomforingEksternV1Dto>(records[0].value.decodeToString()).should {
                        it.id shouldBe gjennomforing2.id
                        it.status shouldBe AVSLUTTET
                    }

                    Json.decodeFromString<TiltaksgjennomforingEksternV1Dto>(records[1].value.decodeToString()).should {
                        it.id shouldBe gjennomforing3.id
                        it.status shouldBe AVSLUTTET
                    }
                }
            }
        }

        test("forsøker ikke å avslutte gjennomføringer som allerede er avsluttet, avlyst eller avbrutt") {
            database.run {
                queries.gjennomforing.setStatus(
                    id = gjennomforing1.id,
                    status = AVSLUTTET,
                    tidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay(),
                    aarsak = null,
                )

                queries.gjennomforing.setStatus(
                    id = gjennomforing2.id,
                    status = AVLYST,
                    tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                    aarsak = AvbruttAarsak.Feilregistrering,
                )

                queries.gjennomforing.setStatus(
                    id = gjennomforing3.id,
                    status = AVBRUTT,
                    tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                    aarsak = AvbruttAarsak.ForFaaDeltakere,
                )
            }

            val task = createTask()

            task.oppdaterGjennomforingStatus(today = LocalDate.of(2024, 1, 2))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(AVSLUTTET, avbrutt = null))
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status.status.shouldBe(AVLYST)
                    it.status.avbrutt.shouldNotBeNull().aarsak.shouldBe(AvbruttAarsak.Feilregistrering)
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status.status.shouldBe(AVBRUTT)
                    it.status.avbrutt.shouldNotBeNull().aarsak.shouldBe(AvbruttAarsak.ForFaaDeltakere)
                }

                queries.kafkaProducerRecord.getRecords(10).shouldBeEmpty()
            }
        }
    }

    context("når gjennomføring blir avsluttet") {
        val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 1, 31),
        )

        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(gjennomforing),
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("avpubliserer og stenger gjennomføring for påmelding") {
            database.run {
                queries.gjennomforing.setPublisert(gjennomforing.id, true)
                queries.gjennomforing.setApentForPamelding(gjennomforing.id, true)
            }

            createTask().oppdaterGjennomforingStatus(today = LocalDate.of(2023, 2, 1))

            database.run {
                queries.gjennomforing.get(gjennomforing.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatusDto(AVSLUTTET, avbrutt = null))
                    it.publisert.shouldBe(false)
                    it.apentForPamelding.shouldBe(false)
                }
            }
        }
    }
})
