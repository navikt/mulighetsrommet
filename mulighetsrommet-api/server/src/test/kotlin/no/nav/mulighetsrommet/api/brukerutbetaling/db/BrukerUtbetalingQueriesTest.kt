package no.nav.mulighetsrommet.api.brukerutbetaling.db

import arrow.core.nonEmptySetOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilskuddFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BrukerUtbetalingQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val behandling = TilskuddFixtures.Behandling

    val domain = MulighetsrommetTestDomain(
        tiltakstyper = listOf(TiltakstypeFixtures.EnkelAmo),
        gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
    )
        { queries.tilskuddBehandling.upsert(behandling) }

    beforeEach {
        domain.initialize(database.api)
    }

    afterEach {
        database.truncateAll()
    }
    val tilskudd = behandling.tilskudd.first()
    val brukerUtbetaling = UpsertBrukerUtbetalingDbo(
        id = UUID.randomUUID(),
        sakId = "SAK-2025-001",
        transaksjonsDato = LocalDate.of(2025, 1, 1),
        belop = 10000,
        tilskuddstype = HelVedUtbetaling.Tilskuddstype.SKOLEPENGER,
        tiltakskode = HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        saksbehandler = NavAnsattFixture.DonaldDuck.navIdent,
        beslutter = NavAnsattFixture.MikkeMus.navIdent,
        besluttetTidspunkt = Instant.parse("2025-01-15T10:00:00Z"),
        tilskuddVedtakId = tilskudd.id,
    )

    test("insert and setBrukerUtbetaling") {
        val behandling = TilskuddFixtures.Behandling
        val tilskudd = TilskuddFixtures.Tilskudd

        database.api.transaction {
            queries.tilskuddBehandling.upsert(behandling.copy(tilskudd = listOf(tilskudd)))
            queries.brukerUtbetaling.insert(brukerUtbetaling)
        }

        val result = database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(tilskudd.id) }

        result.shouldNotBeNull()
        result.id shouldBe brukerUtbetaling.id
        result.sakId shouldBe brukerUtbetaling.sakId
        result.behandlingId shouldBe 1
        result.belop shouldBe brukerUtbetaling.belop
        result.tilskuddstype shouldBe brukerUtbetaling.tilskuddstype
        result.tiltakskode shouldBe brukerUtbetaling.tiltakskode
        result.saksbehandler shouldBe brukerUtbetaling.saksbehandler
        result.beslutter shouldBe brukerUtbetaling.beslutter
    }

    test("getByTilskudd returns null when no utbetaling linked") {
        val behandling = TilskuddFixtures.Behandling
        val tilskudd = TilskuddFixtures.Tilskudd

        database.api.transaction {
            queries.tilskuddBehandling.upsert(behandling.copy(tilskudd = listOf(tilskudd)))
        }

        val result = database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(tilskudd.id) }
        result.shouldBeNull()
    }

    test("setHelVedStatus persists status and error") {
        database.api.transaction {
            queries.brukerUtbetaling.insert(brukerUtbetaling)
        }

        val error = HelVedStatus.StatusError(
            statusCode = 400,
            msg = "Valideringsfeil",
            doc = "https://docs.example.com/error",
        )
        val status = HelVedStatus(
            status = HelVedStatus.Status.FEILET,
            detaljer = null,
            error = error,
        )
        database.api.session { queries.brukerUtbetaling.setHelVedStatus(brukerUtbetaling.id, nonEmptySetOf(1), status) }

        val behandling = TilskuddFixtures.Behandling
        val tilskudd = TilskuddFixtures.Tilskudd
        database.api.transaction {
            queries.tilskuddBehandling.upsert(behandling.copy(tilskudd = listOf(tilskudd)))
        }

        val result = database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(tilskudd.id) }

        result.shouldNotBeNull()
        result.helVedStatus shouldBe HelVedStatus.Status.FEILET
        result.helVedStatusError shouldBe error
    }
})
