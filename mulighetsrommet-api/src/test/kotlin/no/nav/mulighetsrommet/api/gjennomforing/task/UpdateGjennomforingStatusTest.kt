package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V1_TOPIC
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V2_TOPIC
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UpdateGjennomforingStatusTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createTask() = UpdateGjennomforingStatus(
        database.db,
        GjennomforingService(
            config = GjennomforingService.Config(TEST_GJENNOMFORING_V1_TOPIC, TEST_GJENNOMFORING_V2_TOPIC),
            db = database.db,
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

            task.execute(now = LocalDateTime.of(2023, 1, 31, 0, 0))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Gjennomfores
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Gjennomfores
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Gjennomfores
                }

                queries.kafkaProducerRecord.getRecords(10).shouldBeEmpty()
            }
        }

        test("avslutter gjennomføringer når sluttDato er passert") {
            val task = createTask()

            task.execute(now = LocalDateTime.of(2023, 2, 1, 0, 0))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Gjennomfores
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Avsluttet
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Avsluttet
                }
            }
        }

        test("avslutter gjennomføringer når sluttDato er passert (sluttDato passert med flere dager)") {
            val task = createTask()

            task.execute(now = LocalDateTime.of(2023, 3, 1, 0, 0))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Gjennomfores
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Avsluttet
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status shouldBe GjennomforingStatus.Avsluttet
                }
            }
        }

        test("forsøker ikke å avslutte gjennomføringer som allerede er avsluttet, avlyst eller avbrutt") {
            database.run {
                queries.gjennomforing.setStatus(
                    id = gjennomforing1.id,
                    status = AVSLUTTET,
                    tidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay(),
                    aarsaker = null,
                    forklaring = null,
                )

                queries.gjennomforing.setStatus(
                    id = gjennomforing2.id,
                    status = AVLYST,
                    tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )

                queries.gjennomforing.setStatus(
                    id = gjennomforing3.id,
                    status = AVBRUTT,
                    tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE),
                    forklaring = null,
                )
            }

            val task = createTask()

            task.execute(now = LocalDateTime.of(2024, 1, 2, 0, 0))

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.status.shouldBeTypeOf<GjennomforingStatus.Avsluttet>()
                }
                queries.gjennomforing.get(gjennomforing2.id).shouldNotBeNull().should {
                    it.status.shouldBeTypeOf<GjennomforingStatus.Avlyst>()
                        .aarsaker shouldContain AvbrytGjennomforingAarsak.FEILREGISTRERING
                }
                queries.gjennomforing.get(gjennomforing3.id).shouldNotBeNull().should {
                    it.status.shouldBeTypeOf<GjennomforingStatus.Avbrutt>()
                        .aarsaker shouldContain AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE
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

            createTask().execute(now = LocalDateTime.of(2023, 2, 1, 0, 0))

            database.run {
                queries.gjennomforing.get(gjennomforing.id).shouldNotBeNull().should {
                    it.status.shouldBe(GjennomforingStatus.Avsluttet)
                    it.publisert.shouldBe(false)
                    it.apentForPamelding.shouldBe(false)
                }
            }
        }
    }
})
