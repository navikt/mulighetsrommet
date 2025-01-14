package no.nav.mulighetsrommet.api.refusjon.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.Melding
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DeltakerForslagQueriesTest : FunSpec({
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

    test("crud") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DeltakerForslagQueries(session)

            val forslag = DeltakerForslag(
                id = UUID.randomUUID(),
                deltakerId = deltaker.id,
                endring = Melding.Forslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
                status = DeltakerForslag.Status.GODKJENT,
            )

            queries.upsert(forslag)

            val forslagEtterUpsert = queries.getForslagByGjennomforing(
                TiltaksgjennomforingFixtures.Oppfolging1.id,
            )
            forslagEtterUpsert shouldContainExactly mapOf(deltaker.id to listOf(forslag))

            queries.delete(forslag.id)

            val forslagEtterDelete = queries.getForslagByGjennomforing(
                TiltaksgjennomforingFixtures.Oppfolging1.id,
            )
            forslagEtterDelete shouldNotContainKey deltaker.id
        }
    }
})
