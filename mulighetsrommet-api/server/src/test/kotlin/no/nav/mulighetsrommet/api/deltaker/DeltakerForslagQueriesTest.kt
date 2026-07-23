package no.nav.mulighetsrommet.api.deltaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.util.UUID

// TODO: flyttes til "persistence" etter at avhengigheter også er flyttet (avtale, gjennomføring)
class DeltakerForslagQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val deltaker = DeltakerFixtures.createDeltaker(
        gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
        startDato = LocalDate.now(),
        sluttDato = LocalDate.now().plusMonths(1),
        status = DeltakerStatusType.VENTER_PA_OPPSTART,
    )

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
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

            val forslagEtterUpsert = repository.deltakerForslag.getByGjennomforing(
                GjennomforingFixtures.Oppfolging1.id,
            )
            forslagEtterUpsert shouldContainExactly mapOf(deltaker.id to listOf(forslag))

            repository.deltakerForslag.delete(forslag.id)

            val forslagEtterDelete = repository.deltakerForslag.getByGjennomforing(
                GjennomforingFixtures.Oppfolging1.id,
            )
            forslagEtterDelete shouldNotContainKey deltaker.id
        }
    }
})
