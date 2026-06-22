package no.nav.mulighetsrommet.api.brukerutbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilskuddFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BrukerUtbetalingQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val domain = MulighetsrommetTestDomain(
        tiltakstyper = listOf(TiltakstypeFixtures.EnkelAmo),
        avtaler = listOf(AvtaleFixtures.EnkelAmo),
        gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val utbetaling = HelVedUtbetaling(
        id = UUID.randomUUID(),
        sakId = "SAK-2025-001",
        behandlingId = "BEHANDLING-001",
        personIdent = NorskIdent("12345678901"),
        periode = HelVedUtbetaling.Periode(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 31),
        ),
        belop = 10000,
        tilskuddstype = HelVedUtbetaling.Tilskuddstype.SKOLEPENGER,
        tiltakskode = HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        saksbehandler = NavAnsattFixture.DonaldDuck.navIdent,
        beslutter = NavAnsattFixture.MikkeMus.navIdent,
        besluttetTidspunkt = Instant.parse("2025-01-15T10:00:00Z"),
        dryrun = false,
    )

    test("insert and getByTilskudd") {
        val behandling = TilskuddFixtures.Behandling
        val tilskudd = TilskuddFixtures.Tilskudd

        database.db.transaction {
            queries.tilskuddBehandling.upsert(behandling.copy(tilskudd = listOf(tilskudd)))
            queries.helvedUtbetaling.insert(utbetaling)
            queries.tilskuddBehandling.setBrukerUtbetaling(tilskudd.id, utbetaling.id)
        }

        val result = database.db.session { queries.helvedUtbetaling.getByTilskudd(tilskudd.id) }

        result.shouldNotBeNull()
        result.id shouldBe utbetaling.id
        result.sakId shouldBe utbetaling.sakId
        result.behandlingId shouldBe utbetaling.behandlingId
        result.periode shouldBe Periode.fromInclusiveDates(utbetaling.periode.fom, utbetaling.periode.tom)
        result.belop shouldBe utbetaling.belop
        result.tilskuddstype shouldBe utbetaling.tilskuddstype
        result.tiltakskode shouldBe utbetaling.tiltakskode
        result.saksbehandler shouldBe utbetaling.saksbehandler
        result.beslutter shouldBe utbetaling.beslutter
    }

    test("getByTilskudd returns null when no utbetaling linked") {
        val behandling = TilskuddFixtures.Behandling
        val tilskudd = TilskuddFixtures.Tilskudd

        database.db.transaction {
            queries.tilskuddBehandling.upsert(behandling.copy(tilskudd = listOf(tilskudd)))
        }

        val result = database.db.session { queries.helvedUtbetaling.getByTilskudd(tilskudd.id) }
        result.shouldBeNull()
    }

    test("setHelVedStatus persists status and error") {
        database.db.transaction {
            queries.helvedUtbetaling.insert(utbetaling)
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
        database.db.session { queries.helvedUtbetaling.setHelVedStatus(utbetaling.id, status) }

        val behandling = TilskuddFixtures.Behandling
        val tilskudd = TilskuddFixtures.Tilskudd
        database.db.transaction {
            queries.tilskuddBehandling.upsert(behandling.copy(tilskudd = listOf(tilskudd)))
            queries.tilskuddBehandling.setBrukerUtbetaling(tilskudd.id, utbetaling.id)
        }

        val result = database.db.session { queries.helvedUtbetaling.getByTilskudd(tilskudd.id) }

        result.shouldNotBeNull()
        result.helVedStatus shouldBe HelVedStatus.Status.FEILET
        result.helVedStatusError shouldBe error
    }
})
