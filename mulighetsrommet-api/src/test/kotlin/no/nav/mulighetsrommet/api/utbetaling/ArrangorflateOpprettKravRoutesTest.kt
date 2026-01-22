package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.ApplicationConfigLocal
import no.nav.mulighetsrommet.api.arrangorflate.api.DatoVelger
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravInnsendingsInformasjon
import no.nav.mulighetsrommet.api.arrangorflate.dto.TabelloversiktRadDto
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.ArrangorflateTestUtils.hovedenhet
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.fail
import java.time.LocalDate

class ArrangorflateOpprettKravRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val identMedTilgang = ArrangorflateTestUtils.identMedTilgang
    val underenhet = ArrangorflateTestUtils.underenhet
    val orgnr = underenhet.organisasjonsnummer.value

    val deltaker = ArrangorflateTestUtils.createTestDeltaker()
    val tilsagn = ArrangorflateTestUtils.createTestTilsagn()
    val aftGjennomforing = GjennomforingFixtures.AFT1
    // TODO: gjør det tydligere i testene hvilke prismodeller som gjelder per gjennomføring
    val oppfolgingGjennomforing = GjennomforingFixtures.Oppfolging1
    val arrGjennomforing = GjennomforingFixtures.ArbeidsrettetRehabilitering

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        tiltakstyper = listOf(
            TiltakstypeFixtures.AFT,
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.ArbeidsrettetRehabilitering,
        ),
        deltakere = listOf(deltaker),
        arrangorer = listOf(hovedenhet, ArrangorflateTestUtils.underenhet),
        tilsagn = listOf(tilsagn),
        gjennomforinger = listOf(
            aftGjennomforing,
            oppfolgingGjennomforing,
            arrGjennomforing,
        ),
        avtaler = listOf(
            AvtaleFixtures.AFT, // Forhåndsgodkjent
            AvtaleFixtures.oppfolging, // Avtalt pris per time oppfølging
            AvtaleFixtures.ARR, // Annen avtalt pris
        ),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    afterSpec {
        oauth.shutdown()
    }

    val tiltaksoversiktUrl = "/api/arrangorflate/tiltaksoversikt"

    test("tom gjennomføringstabell hvis ingen prismodell er konfigurert") {
        var config = ArrangorflateTestUtils.appConfig(oauth).copy(
            okonomi = ApplicationConfigLocal.okonomi.copy(
                opprettKravPrismodeller = emptyList(),
            ),
        )
        withTestApplication(config) {
            val response = client.get(tiltaksoversiktUrl) {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
            }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<List<TabelloversiktRadDto>>()
            body.shouldBeEmpty()
        }
    }

    test("skal kunne få gjennomføringstabellen med litt data") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response = client.get(tiltaksoversiktUrl) {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
            }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<List<TabelloversiktRadDto>>()
            body.shouldNotBeNull()
            body.size shouldBeGreaterThan 0
        }
    }

    test("Avtalt pris per time oppfølging får liste av tilgjengelige perioder") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response =
                client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/${oppfolgingGjennomforing.id}/opprett-krav/innsendingsinformasjon") {
                    bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                }

            response.status shouldBe HttpStatusCode.OK
            val data = response.body<OpprettKravInnsendingsInformasjon>()
            when (data.datoVelger) {
                is DatoVelger.DatoSelect ->
                    data.datoVelger.periodeForslag.isNotEmpty()

                is DatoVelger.DatoRange ->
                    fail { "Skal vise en liste av perioder for timespris innsending" }
            }
        }
    }

    test("Annen avtalt pris skal kunne velge fritt i datovelger") {
        val config = ArrangorflateTestUtils.appConfig(oauth)
        withTestApplication(config) {
            val response =
                client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/${arrGjennomforing.id}/opprett-krav/innsendingsinformasjon") {
                    bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                }

            response.status shouldBe HttpStatusCode.OK
            val data = response.body<OpprettKravInnsendingsInformasjon>()
            when (data.datoVelger) {
                is DatoVelger.DatoSelect ->
                    fail { "Annen avtalt pris skal ha start- og sluttdato datepicker" }

                is DatoVelger.DatoRange ->
                    // skal være slutt dato for konfigurert tilsagnsperiode
                    data.datoVelger.maksSluttdato shouldBe config.okonomi.gyldigTilsagnPeriode[Tiltakskode.ARBEIDSRETTET_REHABILITERING]!!.slutt
            }
        }
    }

    test("Investeringskrav skal bare kunne velge fra forrige utbetalingsperiode") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response =
                client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/${aftGjennomforing.id}/opprett-krav/innsendingsinformasjon") {
                    bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                }

            response.status shouldBe HttpStatusCode.OK
            val data = response.body<OpprettKravInnsendingsInformasjon>()
            when (data.datoVelger) {
                is DatoVelger.DatoSelect ->
                    fail { "Investeringer skal ha start- og sluttdato datepicker" }

                is DatoVelger.DatoRange ->
                    data.datoVelger.maksSluttdato shouldBe LocalDate.now() // Eksklusiv maks dato
            }
        }
    }
})
