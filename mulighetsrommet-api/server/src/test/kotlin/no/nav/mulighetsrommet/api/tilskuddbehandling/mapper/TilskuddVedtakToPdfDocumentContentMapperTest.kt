package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture.DonaldDuck
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture.MikkeMus
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.HoyereUtdanning
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilskuddFixtures
import no.nav.mulighetsrommet.api.fixtures.setGodkjent
import no.nav.mulighetsrommet.api.fixtures.setTilBehandling
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.hentVedtaksbrevInnhold
import no.nav.mulighetsrommet.api.utbetaling.service.AvvistGrunn
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.Instant

class TilskuddVedtakToPdfDocumentContentMapperTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val tilskudd = listOf(TilskuddFixtures.TilskuddInnvilgelse, TilskuddFixtures.TilskuddAvslag)
    val behandling = TilskuddFixtures.Behandling.copy(gjennomforingId = HoyereUtdanning.id, tilskudd = tilskudd)
    val deltaker = DeltakerFixtures.createDeltaker(
        gjennomforingId = HoyereUtdanning.id,
    )

    val personaliaService = mockk<PersonaliaService>()

    beforeEach {
        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.HoyereUtdanning),
            navEnheter = listOf(NavEnhetFixtures.Innlandet),
            ansatte = listOf(DonaldDuck, MikkeMus),
            arrangorer = listOf(ArrangorFixtures.underenhet1),
            gjennomforinger = listOf(HoyereUtdanning),
            prismodeller = listOf(PrismodellFixtures.TilskuddTilOpplaering),
            deltakere = listOf(
                deltaker,
            ),
        ).initialize(database.api)

        database.api.transaction {
            queries.tilskuddBehandling.upsert(behandling)
        }

        database.api.session {
            setGodkjent(
                uuid = behandling.id,
                type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
                behandletAv = DonaldDuck.navIdent,
                besluttetAv = MikkeMus.navIdent,
                behandletTidspunkt = Instant.parse("2026-05-25T12:00:00Z"),
                besluttetTidspunkt = Instant.parse("2026-05-26T12:00:00Z"),
            )
        }

        coEvery {
            personaliaService.getPersonalia(deltaker.id, any())
        } returns Personalia(
            deltakerId = deltaker.id,
            norskIdent = NorskIdent("12345678901"),
            navn = "Ola Nordmann",
            oppfolgingEnhet = null,
            geografiskEnhet = null,
            region = null,
            gradering = Gradering.UGRADERT,
            avvistGrunn = null,
        )
    }

    afterEach {
        database.truncateAll()
    }

    test("Oppretter pdf for vedtak med både innvilgelse og avslag") {
        val innhold = database.api.session {
            hentVedtaksbrevInnhold(behandling.id, personaliaService).shouldBeRight()
        }

        val pdfContent = TilskuddVedtakToPdfDocumentContentMapper.toPdfDocumentContent(innhold)

        expectSelfie(jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent))
            .toMatchDisk("vedtakInnvilgelseOgAvslag")
    }

    test("Feiler når gjennomføring mangler startdato") {
        database.api.transaction {
            queries.gjennomforing.upsert(HoyereUtdanning.copy(startDato = null))
        }

        val result = database.api.session {
            hentVedtaksbrevInnhold(behandling.id, personaliaService)
        }

        result.shouldBeLeft("Gjennomføring ${HoyereUtdanning.id} mangler startdato")
    }

    test("Feiler når gjennomføring mangler sluttdato") {
        database.api.transaction {
            queries.gjennomforing.upsert(HoyereUtdanning.copy(sluttDato = null))
        }

        val result = database.api.session {
            hentVedtaksbrevInnhold(behandling.id, personaliaService)
        }

        result.shouldBeLeft("Gjennomføring ${HoyereUtdanning.id} mangler sluttdato")
    }

    test("Feiler når deltaker ikke har tilgang til personalia") {
        coEvery {
            personaliaService.getPersonalia(deltaker.id, any())
        } returns Personalia(
            deltakerId = deltaker.id,
            norskIdent = NorskIdent("12345678901"),
            navn = "Ola Nordmann",
            oppfolgingEnhet = null,
            geografiskEnhet = null,
            region = null,
            gradering = Gradering.SKJERMING,
            avvistGrunn = AvvistGrunn.AVVIST_SKJERMING,
        )

        val result = database.api.session {
            hentVedtaksbrevInnhold(behandling.id, personaliaService)
        }

        result.shouldBeLeft("Fikk ikke tilgang til usladdet personalia for ${deltaker.id}")
    }

    test("Feiler når totrinnskontroll ikke er besluttet") {
        database.api.transaction {
            setTilBehandling(
                uuid = behandling.id,
                type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
                behandletAv = DonaldDuck.navIdent,
                behandletTidspunkt = Instant.parse("2026-05-27T12:00:00Z"),
            )
        }

        val result = database.api.session {
            hentVedtaksbrevInnhold(behandling.id, personaliaService)
        }

        result.shouldBeLeft("Totrinnskontroll for tilskudd ${behandling.id} er ikke besluttet")
    }

    test("Feiler når totrinnskontroll mangler saksbehandlernavn") {
        database.api.transaction {
            setGodkjent(
                uuid = behandling.id,
                type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
                behandletAv = NavIdent("UK1"),
                besluttetAv = MikkeMus.navIdent,
                behandletTidspunkt = Instant.parse("2026-05-27T12:00:00Z"),
                besluttetTidspunkt = Instant.parse("2026-05-28T12:00:00Z"),
            )
        }

        val result = database.api.session {
            hentVedtaksbrevInnhold(behandling.id, personaliaService)
        }

        result.shouldBeLeft("Totrinnskontroll for tilskudd ${behandling.id} mangler saksbehandlernavn")
    }

    test("Feiler når totrinnskontroll mangler beslutternavn") {
        database.api.transaction {
            setGodkjent(
                uuid = behandling.id,
                type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
                behandletAv = DonaldDuck.navIdent,
                besluttetAv = NavIdent("Z123456"),
                behandletTidspunkt = Instant.parse("2026-05-27T12:00:00Z"),
                besluttetTidspunkt = Instant.parse("2026-05-28T12:00:00Z"),
            )
        }

        val result = database.api.session {
            hentVedtaksbrevInnhold(behandling.id, personaliaService)
        }

        result.shouldBeLeft("Totrinnskontroll for tilskudd ${behandling.id} mangler beslutternavn")
    }
})
