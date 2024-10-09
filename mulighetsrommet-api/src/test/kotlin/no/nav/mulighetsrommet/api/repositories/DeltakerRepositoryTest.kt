package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import java.time.LocalDateTime
import java.util.*

class DeltakerRepositoryTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1, TiltaksgjennomforingFixtures.Oppfolging2),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    context("consume deltakere") {
        val deltakere = DeltakerRepository(database.db)

        val registrertTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

        val deltaker1 = DeltakerDbo(
            id = UUID.randomUUID(),
            gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            startDato = null,
            sluttDato = null,
            registrertTidspunkt = registrertTidspunkt,
            endretTidspunkt = registrertTidspunkt,
            stillingsprosent = 100.0,
            status = AmtDeltakerStatus(
                AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
                aarsak = null,
                opprettetDato = registrertTidspunkt,
            ),
        )
        val deltaker2 = deltaker1.copy(
            id = UUID.randomUUID(),
            gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id,
        )

        test("CRUD") {
            deltakere.upsert(deltaker1)
            deltakere.upsert(deltaker2)

            deltakere.getAll().shouldContainExactly(deltaker1, deltaker2)

            val avsluttetDeltaker2 = deltaker2.copy(
                status = AmtDeltakerStatus(
                    AmtDeltakerStatus.Type.HAR_SLUTTET,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2023, 3, 2, 0, 0, 0),
                ),
            )
            deltakere.upsert(avsluttetDeltaker2)

            deltakere.getAll().shouldContainExactly(deltaker1, avsluttetDeltaker2)

            deltakere.delete(deltaker1.id)

            deltakere.getAll().shouldContainExactly(avsluttetDeltaker2)
        }

        test("get by tiltaksgjennomforing") {
            deltakere.upsert(deltaker1)
            deltakere.upsert(deltaker2)

            deltakere
                .getAll(tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id)
                .shouldContainExactly(deltaker1)

            deltakere
                .getAll(tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id)
                .shouldContainExactly(deltaker2)
        }
    }
})
