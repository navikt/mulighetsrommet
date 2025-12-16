package no.nav.mulighetsrommet.api.avtale.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvtaleStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class UpdateAvtaleStatusTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createTask() = UpdateAvtaleStatus(
        database.db,
        AvtaleService(
            db = database.db,
            arrangorService = mockk(relaxed = true),
            gjennomforingPublisher = mockk(relaxed = true),
        ),
    )

    val avtale1 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
            startDato = LocalDate.of(2025, 5, 1),
            sluttDato = LocalDate.of(2025, 5, 31),
        ),
        prismodellDbo = AvtaleFixtures.prismodellDbo(),
    )
    val avtale2 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
            startDato = LocalDate.of(2025, 5, 1),
            sluttDato = LocalDate.of(2025, 6, 30),
        ),
        prismodellDbo = AvtaleFixtures.prismodellDbo(),
    )
    val avtale3 = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
            startDato = LocalDate.of(2025, 5, 1),
            sluttDato = null,
        ),
        prismodellDbo = AvtaleFixtures.prismodellDbo(),
    )

    val domain = MulighetsrommetTestDomain(
        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
        avtaler = emptyList(),
    )

    beforeEach {
        domain.initialize(database.db)
        database.run {
            queries.avtale.upsert(avtale1)
            queries.avtale.upsert(avtale2)
            queries.avtale.upsert(avtale3)
        }
    }

    afterEach {
        database.truncateAll()
    }

    test("avslutter ikke avtaler før sluttdato er passert") {
        val task = createTask()

        task.execute(now = LocalDateTime.of(2025, 5, 1, 0, 0))

        database.run {
            queries.avtale.get(avtale1.id).shouldNotBeNull().should {
                it.status.type shouldBe AvtaleStatusType.AKTIV
            }
            queries.avtale.get(avtale2.id).shouldNotBeNull().should {
                it.status.type shouldBe AvtaleStatusType.AKTIV
            }
            queries.avtale.get(avtale3.id).shouldNotBeNull().should {
                it.status.type shouldBe AvtaleStatusType.AKTIV
            }
        }
    }

    test("avslutter avtaler når sluttdato er passert") {
        val task = createTask()

        task.execute(now = LocalDateTime.of(2025, 7, 1, 0, 0))

        database.run {
            queries.avtale.get(avtale1.id).shouldNotBeNull().should {
                it.status.type shouldBe AvtaleStatusType.AVSLUTTET
            }
            queries.avtale.get(avtale2.id).shouldNotBeNull().should {
                it.status.type shouldBe AvtaleStatusType.AVSLUTTET
            }
            queries.avtale.get(avtale3.id).shouldNotBeNull().should {
                it.status.type shouldBe AvtaleStatusType.AKTIV
            }
        }
    }

    test("endre ikke status på avtaler som allerede er avsluttet eller avbrutt") {
        database.run {
            queries.avtale.setStatus(
                id = avtale1.id,
                status = AvtaleStatusType.AVBRUTT,
                tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                aarsaker = listOf(AvbrytAvtaleAarsak.FEILREGISTRERING),
                forklaring = null,
            )

            queries.avtale.setStatus(
                id = avtale2.id,
                status = AvtaleStatusType.AVSLUTTET,
                tidspunkt = null,
                aarsaker = null,
                forklaring = null,
            )
        }

        val task = createTask()

        task.execute(now = LocalDateTime.of(2025, 7, 1, 0, 0))

        database.run {
            queries.avtale.get(avtale1.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatus.Avbrutt(
                    tidspunkt = LocalDate.of(2022, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbrytAvtaleAarsak.FEILREGISTRERING),
                    forklaring = null,
                )
            }
            queries.avtale.get(avtale2.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatus.Avsluttet
            }
            queries.avtale.get(avtale3.id).shouldNotBeNull().should {
                it.status shouldBe AvtaleStatus.Aktiv
            }
        }
    }
})
