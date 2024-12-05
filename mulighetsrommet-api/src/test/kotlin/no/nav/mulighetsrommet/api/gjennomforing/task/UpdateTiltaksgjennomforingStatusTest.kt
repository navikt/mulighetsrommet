package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus.*
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatusDto
import no.nav.mulighetsrommet.notifications.NotificationRepository
import java.time.LocalDate
import java.util.*

class UpdateTiltaksgjennomforingStatusTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    fun createTask(
        tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
    ) = UpdateTiltaksgjennomforingStatus(
        database.db,
        TiltaksgjennomforingService(
            db = database.db,
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
            notificationRepository = NotificationRepository(database.db),
            validator = mockk(relaxed = true),
            documentHistoryService = mockk(relaxed = true),
            navAnsattService = mockk(relaxed = true),
        ),
    )

    context("oppdater statuser på tiltaksgjennomføringer") {
        val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 12, 31),
        )
        val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 1, 31),
        )
        val gjennomforing3 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 1, 31),
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

        val gjennomforinger = TiltaksgjennomforingRepository(database.db)

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        test("forsøker ikke å avslutte gjennomføringer før sluttDato er passert") {
            val producer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val task = createTask(producer)

            task.oppdaterTiltaksgjennomforingStatus(today = LocalDate.of(2023, 1, 31))

            gjennomforinger.get(gjennomforing1.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
            }
            gjennomforinger.get(gjennomforing2.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
            }
            gjennomforinger.get(gjennomforing3.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
            }

            verify(exactly = 0) { producer.publish(any()) }
        }

        test("avslutter gjennomføringer når sluttDato er passert") {
            val producer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val task = createTask(producer)

            task.oppdaterTiltaksgjennomforingStatus(today = LocalDate.of(2023, 2, 1))

            gjennomforinger.get(gjennomforing1.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
            }
            gjennomforinger.get(gjennomforing2.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(AVSLUTTET, avbrutt = null))
            }
            gjennomforinger.get(gjennomforing3.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(AVSLUTTET, avbrutt = null))
            }

            verifyAll {
                producer.publish(
                    match {
                        it.id == gjennomforing2.id && it.status == AVSLUTTET
                    },
                )
                producer.publish(
                    match {
                        it.id == gjennomforing3.id && it.status == AVSLUTTET
                    },
                )
            }
        }

        test("avslutter gjennomføringer når sluttDato er passert (sluttDato passert med flere dager)") {
            val producer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val task = createTask(producer)

            task.oppdaterTiltaksgjennomforingStatus(today = LocalDate.of(2023, 3, 1))

            gjennomforinger.get(gjennomforing1.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(GJENNOMFORES, avbrutt = null))
            }
            gjennomforinger.get(gjennomforing2.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(AVSLUTTET, avbrutt = null))
            }
            gjennomforinger.get(gjennomforing3.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(AVSLUTTET, avbrutt = null))
            }

            verifyAll {
                producer.publish(
                    match {
                        it.id == gjennomforing2.id && it.status == AVSLUTTET
                    },
                )
                producer.publish(
                    match {
                        it.id == gjennomforing3.id && it.status == AVSLUTTET
                    },
                )
            }
        }

        test("forsøker ikke å avslutte gjennomføringer som allerede er avsluttet, avlyst eller avbrutt") {
            val producer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val task = createTask(producer)

            gjennomforinger.setAvsluttet(
                gjennomforing1.id,
                LocalDate.of(2024, 1, 1).atStartOfDay(),
                AvbruttAarsak.Feilregistrering,
            )

            gjennomforinger.setAvsluttet(
                gjennomforing2.id,
                LocalDate.of(2022, 12, 31).atStartOfDay(),
                AvbruttAarsak.Feilregistrering,
            )

            gjennomforinger.setAvsluttet(
                gjennomforing3.id,
                LocalDate.of(2023, 1, 1).atStartOfDay(),
                AvbruttAarsak.Feilregistrering,
            )

            task.oppdaterTiltaksgjennomforingStatus(today = LocalDate.of(2024, 1, 2))

            gjennomforinger.get(gjennomforing1.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(AVSLUTTET, avbrutt = null))
            }
            gjennomforinger.get(gjennomforing2.id).shouldNotBeNull().should {
                it.status.status.shouldBe(AVLYST)
                it.status.avbrutt.shouldNotBeNull().aarsak.shouldBe(AvbruttAarsak.Feilregistrering)
            }
            gjennomforinger.get(gjennomforing3.id).shouldNotBeNull().should {
                it.status.status.shouldBe(AVBRUTT)
                it.status.avbrutt.shouldNotBeNull().aarsak.shouldBe(AvbruttAarsak.Feilregistrering)
            }

            verify(exactly = 0) { producer.publish(any()) }
        }
    }

    context("når gjennomføring blir avsluttet") {
        val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 1, 31),
        )

        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(gjennomforing),
        )

        val gjennomforinger = TiltaksgjennomforingRepository(database.db)

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        test("avpubliserer og stenger gjennomføring for påmelding") {
            val producer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val task = createTask(producer)

            gjennomforinger.setPublisert(gjennomforing.id, true)
            gjennomforinger.setApentForPamelding(gjennomforing.id, true)

            task.oppdaterTiltaksgjennomforingStatus(today = LocalDate.of(2023, 2, 1))

            gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.status.shouldBe(TiltaksgjennomforingStatusDto(AVSLUTTET, avbrutt = null))
                it.publisert.shouldBe(false)
                it.apentForPamelding.shouldBe(false)
            }
        }
    }
})
