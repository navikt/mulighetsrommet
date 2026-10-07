package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilBehandling
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NOK
import java.util.UUID

class TilsagnDtoQueryTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val ansatt = NavAnsattFixture.MikkeMus.medRoller(setOf(NavAnsattRolle.generell(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)))
    val tilsagn = TilsagnFixtures.Tilsagn1
    val personaliaService = mockk<PersonaliaService>()
    val onBehalfOf = mockk<PersonaliaService.OnBehalfOf.NavAnsatt>()

    beforeEach {
        coEvery { personaliaService.getPersonalia(any<List<UUID>>(), onBehalfOf) } returns emptyList()
        MulighetsrommetTestDomain(
            ansatte = listOf(ansatt, NavAnsattFixture.DonaldDuck, NavAnsattFixture.FetterAnton),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(GjennomforingFixtures.AFT1),
            tilsagn = listOf(tilsagn),
        ).initialize(database.api)
    }

    afterEach {
        database.truncateAll()
    }

    val query by lazy { TilsagnDtoQuery(database.api, personaliaService) }

    test("returnerte tilsagn tilbyr redigering og sletting til saksbehandler") {
        database.run {
            setTilsagnStatus(tilsagn, TilsagnStatus.RETURNERT)
            queries.ansatt.save(ansatt.medRoller(setOf(NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK))))
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.REDIGER,
            TilsagnHandling.SLETT,
        )
    }

    test("godkjente tilsagn tilbyr annullering eller oppgjør avhengig av brukt beløp") {
        database.run {
            setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
            queries.ansatt.save(ansatt.medRoller(setOf(NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK))))
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.ANNULLER,
        )

        database.run {
            queries.tilsagn.setBruktBelop(tilsagn.id, 1.NOK)
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.GJOR_OPP,
        )
    }

    test("godkjenning tilbys bare når en annen ansatt har behandlet opprettelsen") {
        database.run {
            setTilsagnStatus(tilsagn, TilsagnStatus.TIL_GODKJENNING)
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.GODKJENN,
            TilsagnHandling.RETURNER,
        )

        database.run {
            setTilBehandling(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, ansatt.navIdent)
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.RETURNER,
        )
    }

    test("godkjenning av annullering bruker behandleren fra annulleringen") {
        database.run {
            setTilsagnStatus(tilsagn, TilsagnStatus.TIL_ANNULLERING, behandletAv = ansatt.navIdent)
            setTilBehandling(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING, NavAnsattFixture.DonaldDuck.navIdent)
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.GODKJENN_ANNULLERING,
            TilsagnHandling.AVSLA_ANNULLERING,
        )

        database.run {
            setTilBehandling(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING, ansatt.navIdent)
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.AVSLA_ANNULLERING,
        )
    }

    test("godkjenning av oppgjør bruker behandleren fra oppgjøret") {
        database.run {
            setTilsagnStatus(tilsagn, TilsagnStatus.TIL_OPPGJOR, behandletAv = ansatt.navIdent)
            setTilBehandling(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR, NavAnsattFixture.DonaldDuck.navIdent)
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.GODKJENN_OPPGJOR,
            TilsagnHandling.AVSLA_OPPGJOR,
        )

        database.run {
            setTilBehandling(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR, ansatt.navIdent)
        }
        query.getDetaljer(tilsagn.id, ansatt.navIdent, onBehalfOf).shouldBeRight().handlinger shouldBe setOf(
            TilsagnHandling.AVSLA_OPPGJOR,
        )
    }
})
