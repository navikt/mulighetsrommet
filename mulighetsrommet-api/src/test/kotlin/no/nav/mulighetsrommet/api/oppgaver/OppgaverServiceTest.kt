
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.oppgaver.OppgaverFilter
import no.nav.mulighetsrommet.oppgaver.OppgaverService

class OppgaverServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    test("Skal hente oppgaver for tilsagn med filter") {
        val service = OppgaverService(database.db)
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            tilsagn = listOf(
                TilsagnFixtures.Tilsagn1,
            ),
        )

        domain.initialize(database.db)

        val oppgaver = service.getOppgaverForTilsagn(
            filter = OppgaverFilter(
                oppgavetyper = emptyList(),
                tiltakstyper = emptyList(),
                regioner = emptyList(),
            ),
            ansattRoller = setOf(
                NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
                NavAnsattRolle.OKONOMI_BESLUTTER,
            ),
        )

        oppgaver.size shouldBe 1
    }

    test("Skal bare returnere oppgaver for tilsagn til godkjenning og annullering når ansatt har korrekt rolle") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            tilsagn = listOf(
                TilsagnFixtures.Tilsagn1,
                TilsagnFixtures.Tilsagn2,
                TilsagnFixtures.Tilsagn3,
            ),
        ) {
            queries.tilsagn.godkjenn(
                id = TilsagnFixtures.Tilsagn2.id,
                navIdent = NavIdent("Z123456"),
            )
            queries.tilsagn.tilAnnullering(
                id = TilsagnFixtures.Tilsagn2.id,
                navIdent = NavIdent("Z123456"),
                aarsaker = emptyList(),
                forklaring = null,
            )
            queries.tilsagn.godkjennAnnullering(
                id = TilsagnFixtures.Tilsagn2.id,
                navIdent = NavIdent("G523456"),
            )
        }

        domain.initialize(database.db)

        val service = OppgaverService(database.db)
        val oppgaver = service.getOppgaverForTilsagn(
            filter = OppgaverFilter(
                oppgavetyper = emptyList(),
                tiltakstyper = emptyList(),
                regioner = emptyList(),
            ),
            ansattRoller = setOf(
                NavAnsattRolle.OKONOMI_BESLUTTER,
            ),
        )

        oppgaver.size shouldBe 2
    }

    test("Skal bare returnere oppgaver som er returnert til ansatte uten beslutterrolle") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            tilsagn = listOf(
                TilsagnFixtures.Tilsagn1,
                TilsagnFixtures.Tilsagn2,
                TilsagnFixtures.Tilsagn3,
            ),
        ) {
            queries.tilsagn.returner(
                id = TilsagnFixtures.Tilsagn3.id,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
                aarsaker = emptyList(),
                forklaring = null,
            )
        }

        domain.initialize(database.db)

        val service = OppgaverService(database.db)

        val oppgaver = service.getOppgaverForTilsagn(
            filter = OppgaverFilter(
                oppgavetyper = emptyList(),
                tiltakstyper = emptyList(),
                regioner = emptyList(),
            ),
            ansattRoller = setOf(
                NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
            ),
        )

        oppgaver.size shouldBe 1
    }

    test("Skal bare returnere oppgaver for valgt region") {
        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            enheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.Oslo),
            tilsagn = listOf(
                TilsagnFixtures.Tilsagn1,
                TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
                TilsagnFixtures.Tilsagn3.copy(kostnadssted = NavEnhetFixtures.Oslo.enhetsnummer),
            ),
        ).initialize(database.db)

        val service = OppgaverService(database.db)

        val oppgaver = service.getOppgaverForTilsagn(
            filter = OppgaverFilter(
                oppgavetyper = emptyList(),
                tiltakstyper = emptyList(),
                regioner = listOf(NavEnhetFixtures.Innlandet.enhetsnummer),
            ),
            ansattRoller = setOf(
                NavAnsattRolle.OKONOMI_BESLUTTER,
            ),
        )

        oppgaver.size shouldBe 1
    }

    test("Skal ikke se oppgaver hvis du ikke har korrekte roller") {
        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            enheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.Oslo),
            tilsagn = listOf(
                TilsagnFixtures.Tilsagn1,
                TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
                TilsagnFixtures.Tilsagn3.copy(kostnadssted = NavEnhetFixtures.Oslo.enhetsnummer),
            ),
        ).initialize(database.db)

        val service = OppgaverService(database.db)

        val oppgaver = service.getOppgaverForTilsagn(
            filter = OppgaverFilter(
                oppgavetyper = emptyList(),
                tiltakstyper = emptyList(),
                regioner = listOf(NavEnhetFixtures.Innlandet.enhetsnummer),
            ),
            ansattRoller = emptySet(),
        )

        oppgaver.size shouldBe 0
    }
})
