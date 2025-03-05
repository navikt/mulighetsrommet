package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesmengdeDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DeltakerQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1, GjennomforingFixtures.Oppfolging2),
    )

    val registrertTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

    val deltaker1 = DeltakerDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
        startDato = null,
        sluttDato = null,
        registrertTidspunkt = registrertTidspunkt,
        endretTidspunkt = registrertTidspunkt,
        deltakelsesprosent = 100.0,
        status = DeltakerStatus(
            DeltakerStatus.Type.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = registrertTidspunkt,
        ),
        deltakelsesmengder = emptyList(),
    )
    val deltaker2 = deltaker1.copy(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.Oppfolging2.id,
    )

    test("CRUD") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DeltakerQueries(session)

            queries.upsert(deltaker1)
            queries.upsert(deltaker2)

            queries.getAll().shouldContainExactly(deltaker1.toDto(), deltaker2.toDto())

            val avsluttetDeltaker2 = deltaker2.copy(
                status = DeltakerStatus(
                    DeltakerStatus.Type.HAR_SLUTTET,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2023, 3, 2, 0, 0, 0),
                ),
            )
            queries.upsert(avsluttetDeltaker2)

            queries.getAll().shouldContainExactly(deltaker1.toDto(), avsluttetDeltaker2.toDto())

            queries.delete(deltaker1.id)

            queries.getAll().shouldContainExactly(avsluttetDeltaker2.toDto())
        }
    }

    test("deltakelsesmengder blir overskrevet og hentet i riktig rekkefølge") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DeltakerQueries(session)

            queries.upsert(
                deltaker1.copy(
                    deltakelsesmengder = listOf(
                        DeltakerDbo.Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2023, 3, 1),
                            deltakelsesprosent = 100.0,
                            opprettetTidspunkt = registrertTidspunkt,
                        ),
                    ),
                ),
            )
            queries.getDeltakelsesmengder(deltaker1.id) shouldBe listOf(
                DeltakelsesmengdeDto(LocalDate.of(2023, 3, 1), 100.0),
            )

            queries.upsert(
                deltaker1.copy(
                    deltakelsesmengder = listOf(
                        DeltakerDbo.Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2023, 3, 10),
                            deltakelsesprosent = 100.0,
                            opprettetTidspunkt = registrertTidspunkt,
                        ),
                        DeltakerDbo.Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2023, 3, 5),
                            deltakelsesprosent = 100.0,
                            opprettetTidspunkt = registrertTidspunkt,
                        ),
                    ),
                ),
            )
            queries.getDeltakelsesmengder(deltaker1.id) shouldBe listOf(
                DeltakelsesmengdeDto(LocalDate.of(2023, 3, 5), 100.0),
                DeltakelsesmengdeDto(LocalDate.of(2023, 3, 10), 100.0),
            )
        }
    }

    test("get by gjennomforing") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DeltakerQueries(session)

            queries.upsert(deltaker1)
            queries.upsert(deltaker2)

            queries
                .getAll(gjennomforingId = GjennomforingFixtures.Oppfolging1.id)
                .shouldContainExactly(deltaker1.toDto())

            queries
                .getAll(gjennomforingId = GjennomforingFixtures.Oppfolging2.id)
                .shouldContainExactly(deltaker2.toDto())
        }
    }
})

fun DeltakerDbo.toDto() = DeltakerDto(
    id = id,
    gjennomforingId = gjennomforingId,
    norskIdent = null,
    startDato = startDato,
    sluttDato = startDato,
    registrertTidspunkt = registrertTidspunkt,
    endretTidspunkt = endretTidspunkt,
    deltakelsesprosent = deltakelsesprosent,
    status = status,
)
