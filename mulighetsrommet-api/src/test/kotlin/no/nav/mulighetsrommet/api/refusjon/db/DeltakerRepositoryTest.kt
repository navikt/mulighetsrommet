package no.nav.mulighetsrommet.api.refusjon.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.refusjon.model.DeltakerDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import java.time.LocalDateTime
import java.util.*

class DeltakerRepositoryTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(databaseConfig))

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

        test("CRUD") {
            deltakere.upsert(deltaker1)
            deltakere.upsert(deltaker2)

            deltakere.getAll().shouldContainExactly(deltaker1.toDto(), deltaker2.toDto())

            val avsluttetDeltaker2 = deltaker2.copy(
                status = DeltakerStatus(
                    DeltakerStatus.Type.HAR_SLUTTET,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2023, 3, 2, 0, 0, 0),
                ),
            )
            deltakere.upsert(avsluttetDeltaker2)

            deltakere.getAll().shouldContainExactly(deltaker1.toDto(), avsluttetDeltaker2.toDto())

            deltakere.delete(deltaker1.id)

            deltakere.getAll().shouldContainExactly(avsluttetDeltaker2.toDto())
        }

        test("get by tiltaksgjennomforing") {
            deltakere.upsert(deltaker1)
            deltakere.upsert(deltaker2)

            deltakere
                .getAll(tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id)
                .shouldContainExactly(deltaker1.toDto())

            deltakere
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
