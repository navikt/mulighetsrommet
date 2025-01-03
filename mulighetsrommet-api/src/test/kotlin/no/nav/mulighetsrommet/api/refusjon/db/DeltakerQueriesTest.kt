package no.nav.mulighetsrommet.api.refusjon.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.refusjon.model.DeltakerDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import java.time.LocalDateTime
import java.util.*

class DeltakerQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1, TiltaksgjennomforingFixtures.Oppfolging2),
    )

    val registrertTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

    val deltaker1 = DeltakerDbo(
        id = UUID.randomUUID(),
        gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
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
    )
    val deltaker2 = deltaker1.copy(
        id = UUID.randomUUID(),
        gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id,
    )

    val queries = DeltakerQueries

    test("CRUD") {
        database.runAndRollback {
            domain.setup()

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

    test("get by tiltaksgjennomforing") {
        database.runAndRollback {
            domain.setup()

            queries.upsert(deltaker1)
            queries.upsert(deltaker2)

            queries
                .getAll(tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id)
                .shouldContainExactly(deltaker1.toDto())

            queries
                .getAll(tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id)
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
