package no.nav.mulighetsrommet.api.refusjon.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.Melding
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DeltakerForslagRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val deltaker = DeltakerDbo(
        id = UUID.randomUUID(),
        gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
        startDato = null,
        sluttDato = null,
        registrertTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
        endretTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
        deltakelsesprosent = 100.0,
        status = DeltakerStatus(
            DeltakerStatus.Type.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
        ),
    )

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1),
        deltakere = listOf(deltaker),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    test("crud") {
        val repository = DeltakerForslagRepository(database.db)
        val forslag = DeltakerForslag(
            id = UUID.randomUUID(),
            deltakerId = deltaker.id,
            endring = Melding.Forslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
            status = DeltakerForslag.Status.GODKJENT,
        )

        repository.upsert(forslag)

        repository.getForslagByGjennomforing(TiltaksgjennomforingFixtures.Oppfolging1.id)[deltaker.id] shouldBe listOf(forslag)

        repository.delete(forslag.id)

        repository.getForslagByGjennomforing(TiltaksgjennomforingFixtures.Oppfolging1.id).containsKey(deltaker.id) shouldBe false
    }
})
