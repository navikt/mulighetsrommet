package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import no.nav.amt.model.AmtArrangorMelding
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.util.UUID

class DeltakerForslagQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val deltaker = DeltakerFixtures.createDeltakerDbo(
        GjennomforingFixtures.Oppfolging1.id,
        startDato = LocalDate.now(),
        sluttDato = LocalDate.now().plusMonths(1),
        statusType = DeltakerStatusType.VENTER_PA_OPPSTART,
    )

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
        deltakere = listOf(deltaker),
    )

    test("crud") {
        database.runAndRollback { session ->
            domain.setup(session)

            val forslag = DeltakerForslag(
                id = UUID.randomUUID(),
                deltakerId = deltaker.id,
                endring = AmtArrangorMelding.Forslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
                status = DeltakerForslag.Status.GODKJENT,
            )

            queries.deltakerForslag.upsert(forslag)

            val forslagEtterUpsert = queries.deltakerForslag.getForslagByGjennomforing(
                GjennomforingFixtures.Oppfolging1.id,
            )
            forslagEtterUpsert shouldContainExactly mapOf(deltaker.id to listOf(forslag))

            queries.deltakerForslag.delete(forslag.id)

            val forslagEtterDelete = queries.deltakerForslag.getForslagByGjennomforing(
                GjennomforingFixtures.Oppfolging1.id,
            )
            forslagEtterDelete shouldNotContainKey deltaker.id
        }
    }
})
