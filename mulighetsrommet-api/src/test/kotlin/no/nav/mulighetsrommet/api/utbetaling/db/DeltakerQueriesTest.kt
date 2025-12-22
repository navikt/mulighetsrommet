package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utbetaling.model.Deltakelsesmengde
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DeltakerQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(
            GjennomforingFixtures.Oppfolging1,
            GjennomforingFixtures.Oppfolging1.copy(id = UUID.randomUUID()),
        ),
    )

    val opprettetTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

    val deltaker1 = DeltakerDbo(
        id = UUID.randomUUID(),
        gjennomforingId = domain.gjennomforinger[0].id,
        startDato = null,
        sluttDato = null,
        registrertDato = opprettetTidspunkt.toLocalDate(),
        endretTidspunkt = opprettetTidspunkt,
        deltakelsesprosent = 100.0,
        status = DeltakerStatus(
            DeltakerStatusType.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = opprettetTidspunkt,
        ),
        deltakelsesmengder = emptyList(),
    )
    val deltaker2 = deltaker1.copy(
        id = UUID.randomUUID(),
        gjennomforingId = domain.gjennomforinger[1].id,
    )

    test("CRUD") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.deltaker.upsert(deltaker1)
            queries.deltaker.upsert(deltaker2)

            queries.deltaker.getAll().shouldContainExactly(deltaker1.toDto(), deltaker2.toDto())

            val avsluttetDeltaker2 = deltaker2.copy(
                status = DeltakerStatus(
                    DeltakerStatusType.HAR_SLUTTET,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2023, 3, 2, 0, 0, 0),
                ),
            )
            queries.deltaker.upsert(avsluttetDeltaker2)

            queries.deltaker.getAll().shouldContainExactly(deltaker1.toDto(), avsluttetDeltaker2.toDto())

            queries.deltaker.delete(deltaker1.id)

            queries.deltaker.getAll().shouldContainExactly(avsluttetDeltaker2.toDto())
        }
    }

    test("deltakelsesmengder blir overskrevet og hentet i riktig rekkefølge") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.deltaker.upsert(
                deltaker1.copy(
                    deltakelsesmengder = listOf(
                        DeltakerDbo.Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2023, 3, 1),
                            deltakelsesprosent = 100.0,
                            opprettetTidspunkt = opprettetTidspunkt,
                        ),
                    ),
                ),
            )
            queries.deltaker.getDeltakelsesmengder(deltaker1.id) shouldBe listOf(
                Deltakelsesmengde(LocalDate.of(2023, 3, 1), 100.0),
            )

            queries.deltaker.upsert(
                deltaker1.copy(
                    deltakelsesmengder = listOf(
                        DeltakerDbo.Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2023, 3, 10),
                            deltakelsesprosent = 100.0,
                            opprettetTidspunkt = opprettetTidspunkt,
                        ),
                        DeltakerDbo.Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2023, 3, 5),
                            deltakelsesprosent = 100.0,
                            opprettetTidspunkt = opprettetTidspunkt,
                        ),
                    ),
                ),
            )
            queries.deltaker.getDeltakelsesmengder(deltaker1.id) shouldBe listOf(
                Deltakelsesmengde(LocalDate.of(2023, 3, 5), 100.0),
                Deltakelsesmengde(LocalDate.of(2023, 3, 10), 100.0),
            )

            queries.deltaker.delete(deltaker1.id)
            queries.deltaker.getDeltakelsesmengder(deltaker1.id) shouldBe listOf()
        }
    }

    test("get by gjennomforing") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.deltaker.upsert(deltaker1)
            queries.deltaker.upsert(deltaker2)

            queries
                .deltaker.getAll(gjennomforingId = domain.gjennomforinger[0].id)
                .shouldContainExactly(deltaker1.toDto())

            queries
                .deltaker.getAll(gjennomforingId = domain.gjennomforinger[1].id)
                .shouldContainExactly(deltaker2.toDto())
        }
    }
})

fun DeltakerDbo.toDto() = Deltaker(
    id = id,
    gjennomforingId = gjennomforingId,
    startDato = startDato,
    sluttDato = startDato,
    registrertDato = registrertDato,
    endretTidspunkt = endretTidspunkt,
    deltakelsesprosent = deltakelsesprosent,
    status = status,
)
