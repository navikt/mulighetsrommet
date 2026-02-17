package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingAvtaleService
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V2_TOPIC
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType.AVBRUTT
import no.nav.mulighetsrommet.model.GjennomforingStatusType.AVLYST
import no.nav.mulighetsrommet.model.GjennomforingStatusType.AVSLUTTET
import no.nav.mulighetsrommet.model.GjennomforingStatusType.GJENNOMFORES
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class UpdateGjennomforingStatusTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createTask() = UpdateGjennomforingStatus(
        database.db,
        GjennomforingAvtaleService(
            config = GjennomforingAvtaleService.Config(TEST_GJENNOMFORING_V2_TOPIC),
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
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing1.id).should {
                    it.status shouldBe GJENNOMFORES
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing2.id).should {
                    it.status shouldBe GJENNOMFORES
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing3.id).should {
                    it.status shouldBe GJENNOMFORES
                }

                queries.kafkaProducerRecord.getRecords(10).shouldBeEmpty()
            }
        }

        test("avslutter gjennomføringer når sluttDato er passert") {
            val task = createTask()

            task.execute(now = LocalDateTime.of(2023, 2, 1, 0, 0))

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing1.id).should {
                    it.status shouldBe GJENNOMFORES
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing2.id).should {
                    it.status shouldBe AVSLUTTET
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing3.id).should {
                    it.status shouldBe AVSLUTTET
                }
            }
        }

        test("avslutter gjennomføringer når sluttDato er passert (sluttDato passert med flere dager)") {
            val task = createTask()

            task.execute(now = LocalDateTime.of(2023, 3, 1, 0, 0))

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing1.id).should {
                    it.status shouldBe GJENNOMFORES
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing2.id).should {
                    it.status shouldBe AVSLUTTET
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing3.id).should {
                    it.status shouldBe AVSLUTTET
                }
            }
        }

        test("forsøker ikke å avslutte gjennomføringer som allerede er avsluttet, avlyst eller avbrutt") {
            database.run {
                queries.gjennomforing.setStatus(
                    id = gjennomforing1.id,
                    status = AVSLUTTET,
                    sluttDato = LocalDate.of(2024, 1, 1),
                    aarsaker = null,
                    forklaring = null,
                )

                queries.gjennomforing.setStatus(
                    id = gjennomforing2.id,
                    status = AVLYST,
                    sluttDato = LocalDate.of(2022, 12, 31),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )

                queries.gjennomforing.setStatus(
                    id = gjennomforing3.id,
                    status = AVBRUTT,
                    sluttDato = LocalDate.of(2022, 12, 31),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE),
                    forklaring = null,
                )
            }

            val task = createTask()

            task.execute(now = LocalDateTime.of(2024, 1, 2, 0, 0))

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing1.id).should {
                    it.status shouldBe AVSLUTTET
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing2.id).status shouldBe AVLYST
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing2.id)
                    .avbrytelse!!.aarsaker shouldContain AvbrytGjennomforingAarsak.FEILREGISTRERING
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing3.id).status shouldBe AVBRUTT
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing3.id)
                    .avbrytelse!!.aarsaker shouldContain AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE

                queries.kafkaProducerRecord.getRecords(10).shouldBeEmpty()
            }
        }
    }
})
