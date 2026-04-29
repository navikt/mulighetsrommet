package no.nav.mulighetsrommet.api.tilskuddbehandling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

class TilskuddBehandlingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val ansatt1 = NavAnsattFixture.DonaldDuck.navIdent
    val ansatt2 = NavAnsattFixture.MikkeMus.navIdent

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(GjennomforingFixtures.AFT1),
        ).initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val gyldigRequest = TilskuddBehandlingRequest(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        soknadJournalpostId = "J-2024-001",
        soknadDato = LocalDate.of(2024, 1, 15),
        periodeStart = "2024-01-01",
        periodeSlutt = "2024-07-01",
        kostnadssted = NavEnhetNummer("0502"),
        tilskudd = listOf(
            TilskuddBehandlingRequest.TilskuddRequest(
                id = UUID.randomUUID(),
                tilskuddOpplaeringType = TilskuddOpplaeringType.SKOLEPENGER,
                soknadBelop = ValutaBelopRequest(belop = 50000, valuta = Valuta.NOK),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = "Universitetet i Oslo",
            ),
        ),
    )

    context("godkjenn") {
        test("kan ikke attestere sin egen behandling") {
            val service = TilskuddBehandlingService(database.db)

            service.upsert(gyldigRequest, ansatt1).shouldBeRight()

            service.godkjenn(gyldigRequest.id, ansatt1).shouldBeLeft().shouldHaveSize(1).first().should {
                it.detail shouldBe "Du kan ikke beslutte en tilskuddsbehandling du selv har opprettet"
            }
        }

        test("annen ansatt kan attestere behandling") {
            val service = TilskuddBehandlingService(database.db)

            service.upsert(gyldigRequest, ansatt1).shouldBeRight()

            service.godkjenn(gyldigRequest.id, ansatt2).shouldBeRight()
            service.getDetaljerDto(gyldigRequest.id, ansatt1)?.behandling?.status?.type shouldBe TilskuddBehandlingStatus.FERDIG_BEHANDLET
        }
    }
})
