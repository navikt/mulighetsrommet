package no.nav.mulighetsrommet.api.avtale.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatusDto
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.AvtaleStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UpdateAvtaleStatusTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createTask() = UpdateAvtaleStatus(
        database.db,
        AvtaleService(
            db = database.db,
            validator = mockk(relaxed = true),
            gjennomforingPublisher = mockk(relaxed = true),
        ),
    )

    val avtale1 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        startDato = LocalDate.of(2025, 5, 1),
        sluttDato = LocalDate.of(2025, 5, 31),
        status = AvtaleStatus.AKTIV,
    )
    val avtale2 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        startDato = LocalDate.of(2025, 5, 1),
        sluttDato = LocalDate.of(2025, 6, 30),
        status = AvtaleStatus.AKTIV,
    )
    val avtale3 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        startDato = LocalDate.of(2025, 5, 1),
        sluttDato = null,
        status = AvtaleStatus.AKTIV,
    )

    val domain = MulighetsrommetTestDomain(
        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
        avtaler = listOf(avtale1, avtale2, avtale3),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    test("avslutter ikke avtaler før sluttdato er passert") {
        val task = createTask()

        task.execute(now = LocalDateTime.of(2025, 5, 1, 0, 0))

        database.run {
            queries.avtale.get(avtale1.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Aktiv
            }
            queries.avtale.get(avtale2.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Aktiv
            }
            queries.avtale.get(avtale3.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Aktiv
            }
        }
    }

    test("avslutter avtaler når sluttdato er passert") {
        val task = createTask()

        task.execute(now = LocalDateTime.of(2025, 7, 1, 0, 0))

        database.run {
            queries.avtale.get(avtale1.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Avsluttet
            }
            queries.avtale.get(avtale2.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Avsluttet
            }
            queries.avtale.get(avtale3.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Aktiv
            }
        }
    }

    test("endre ikke status på avtaler som allerede er avsluttet eller avbrutt") {
        database.run {
            queries.avtale.setStatus(
                id = avtale1.id,
                status = AvtaleStatus.AVBRUTT,
                tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                AarsakerOgForklaringRequest(listOf(AvbruttAarsak.FEILREGISTRERING), null),
            )

            queries.avtale.setStatus(
                id = avtale2.id,
                status = AvtaleStatus.AVSLUTTET,
                tidspunkt = null,
                null,
            )
        }

        val task = createTask()

        task.execute(now = LocalDateTime.of(2025, 7, 1, 0, 0))

        database.run {
            queries.avtale.get(avtale1.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Avbrutt(
                    tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbruttAarsak.FEILREGISTRERING),
                    forklaring = null,
                )
            }
            queries.avtale.get(avtale2.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Avsluttet
            }
            queries.avtale.get(avtale3.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatusDto.Aktiv
            }
        }
    }
})
