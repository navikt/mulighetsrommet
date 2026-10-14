package no.nav.mulighetsrommet.api.persistence.deltaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.persistence.AdminTestDomain
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.util.UUID

class DeltakerForslagQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    val gjennomforingId = GjennomforingDboFixtures.Oppfolging1.id

    val deltaker = DeltakerFixtures.createDeltaker(
        gjennomforingId = gjennomforingId,
        startDato = LocalDate.now(),
        sluttDato = LocalDate.now().plusMonths(1),
        status = DeltakerStatusType.VENTER_PA_OPPSTART,
    )

    val domain = AdminTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(GjennomforingDboFixtures.Oppfolging1),
        deltakere = listOf(deltaker),
    )

    test("crud") {
        database.runAndRollback {
            domain.initialize()

            val forslag = DeltakerForslag.fraDeltaker(
                deltaker = deltaker,
                id = UUID.randomUUID(),
                endring = DeltakerForslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
                status = DeltakerForslag.Status.GODKJENT,
            )

            repository.deltakerForslag.save(forslag)

            val forslagEtterUpsert = repository.deltakerForslag.getByGjennomforing(gjennomforingId)
            forslagEtterUpsert shouldContainExactly listOf(forslag)

            repository.deltakerForslag.delete(forslag.id)

            val forslagEtterDelete = repository.deltakerForslag.getByGjennomforing(gjennomforingId)
            forslagEtterDelete shouldContainExactly emptyList()
        }
    }
})
