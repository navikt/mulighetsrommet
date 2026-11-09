package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingAvtaleService
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import java.time.LocalDate

class UpdateApentForPameldingTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    context("steng påmelding for tiltak med felles oppstart") {
        val startDato = LocalDate.now()
        val sluttDato = LocalDate.now().plusMonths(1)

        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering, TiltakstypeFixtures.GruppeAmo),
            avtaler = listOf(AvtaleFixtures.gruppeFagYrke, AvtaleFixtures.gruppeAmo),
            gjennomforinger = listOf(
                GjennomforingDboFixtures.GruppeFagYrke1.copy(
                    startDato = startDato,
                    sluttDato = sluttDato,
                    oppstart = GjennomforingOppstartstype.LOPENDE,
                ),
                GjennomforingDboFixtures.GruppeAmo1.copy(
                    startDato = startDato,
                    sluttDato = sluttDato,
                    oppstart = GjennomforingOppstartstype.FELLES,
                ),
            ),
        ) {
            queries.gjennomforing.setApentForPamelding(GjennomforingDboFixtures.GruppeFagYrke1.id, true)
            queries.gjennomforing.setApentForPamelding(GjennomforingDboFixtures.GruppeAmo1.id, true)
        }

        val service = GjennomforingAvtaleService(
            db = database.admin,
            navAnsattService = mockk(),
        )

        val updateApentForPamelding = UpdateApentForPamelding(
            config = UpdateApentForPamelding.Config(disabled = true),
            db = database.api,
            avtaleGjennomforingService = service,
        )

        beforeEach {
            domain.initialize(database.api)
        }

        afterEach {
            database.truncateAll()
        }

        test("beholder påmelding når dato er før startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = startDato.minusDays(1))

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleOrError(GjennomforingDboFixtures.GruppeFagYrke1.id).should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(GjennomforingDboFixtures.GruppeAmo1.id).should {
                    it.apentForPamelding shouldBe true
                }
            }
        }

        test("stenger påmelding for tiltak med felles oppstart når dato er lik startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now())

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleOrError(GjennomforingDboFixtures.GruppeFagYrke1.id).should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(GjennomforingDboFixtures.GruppeAmo1.id).should {
                    it.apentForPamelding shouldBe false
                }
            }
        }

        test("beholder påmelding når dato er etter startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now().plusDays(1))

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleOrError(GjennomforingDboFixtures.GruppeFagYrke1.id).should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.getGjennomforingAvtaleOrError(GjennomforingDboFixtures.GruppeAmo1.id).should {
                    it.apentForPamelding shouldBe true
                }
            }
        }
    }
})
