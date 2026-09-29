package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingStatusDto
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingTilstandsendringDbo
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.util.UUID

class UtbetalingDtoQueryTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val navIdent = NavAnsattFixture.DonaldDuck.navIdent
    fun createQuery() = UtbetalingDtoQuery(database.api, mockk())

    afterEach {
        database.truncateAll()
    }

    test("utbetalingsdetaljer krever lesetilgang") {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            utbetalinger = listOf(utbetaling1),
        ).initialize(database.api)

        createQuery().getDetaljer(utbetaling1.id, navIdent).shouldBeLeft(
            UtbetalingDtoQueryError.ManglerTilgang("Du mangler lesetilgang til utbetalingen"),
        )
    }

    test("utbetalingsdetaljer inneholder status og avbrytelse til beslutning") {
        val totrinnskontroll = Totrinnskontroll.opprett(
            id = UUID.randomUUID(),
            entityId = utbetaling1.id,
            type = TotrinnskontrollType.UTBETALING_AVBRYTELSE,
            behandletAv = NavAnsattFixture.MikkeMus.navIdent,
        )
        MulighetsrommetTestDomain(
            ansatte = listOf(
                NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.generell(Rolle.OKONOMI_LES))),
                NavAnsattFixture.MikkeMus,
            ),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            utbetalinger = listOf(utbetaling1),
        ) {
            queries.totrinnskontroll.upsert(totrinnskontroll)
            queries.utbetaling.upsert(
                utbetaling1.copy(
                    status = UtbetalingStatusType.TIL_AVBRYTELSE,
                    avbrytelse = UtbetalingTilstandsendringDbo(totrinnskontroll.id, UtbetalingStatusType.GENERERT),
                ),
            )
        }.initialize(database.api)

        val detaljer = createQuery().getDetaljer(utbetaling1.id, navIdent).shouldBeRight()

        detaljer.utbetaling.id shouldBe utbetaling1.id
        detaljer.utbetaling.status shouldBe UtbetalingStatusDto.fromUtbetalingStatus(
            UtbetalingStatusType.TIL_AVBRYTELSE,
            emptySet(),
            totrinnskontroll,
        )
        detaljer.utbetaling.avbrytelse.shouldBeTypeOf<TotrinnskontrollDto.TilBeslutning>()
    }

    test("enkeltplass gir handling SLETT, men ikke SEND_TIL_AVBRYTELSE") {
        val enkeltplassUtbetaling = utbetaling1.copy(
            id = UUID.randomUUID(),
            gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
            status = UtbetalingStatusType.TIL_BEHANDLING,
        )

        MulighetsrommetTestDomain(
            ansatte = listOf(
                NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS))),
                NavAnsattFixture.MikkeMus,
            ),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
            utbetalinger = listOf(enkeltplassUtbetaling),
        ).initialize(database.api)

        val detaljer = createQuery().getDetaljer(enkeltplassUtbetaling.id, navIdent).shouldBeRight()
        detaljer.handlinger shouldContain UtbetalingHandling.SLETT
        detaljer.handlinger shouldNotContain UtbetalingHandling.SEND_TIL_AVBRYTELSE
    }
})
