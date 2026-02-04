package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.service.AvtaleGjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V2_TOPIC
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import java.time.LocalDate

class UpdateApentForPameldingTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("steng påmelding for tiltak med felles oppstart") {
        val startDato = LocalDate.now()
        val sluttDato = LocalDate.now().plusMonths(1)

        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering, TiltakstypeFixtures.GruppeAmo),
            avtaler = listOf(AvtaleFixtures.gruppeFagYrke, AvtaleFixtures.gruppeAmo),
            gjennomforinger = listOf(
                GjennomforingFixtures.GruppeFagYrke1.copy(
                    startDato = startDato,
                    sluttDato = sluttDato,
                    oppstart = GjennomforingOppstartstype.LOPENDE,
                ),
                GjennomforingFixtures.GruppeAmo1.copy(
                    startDato = startDato,
                    sluttDato = sluttDato,
                    oppstart = GjennomforingOppstartstype.FELLES,
                ),
            ),
        ) {
            queries.gjennomforing.setApentForPamelding(GjennomforingFixtures.GruppeFagYrke1.id, true)
            queries.gjennomforing.setApentForPamelding(GjennomforingFixtures.GruppeAmo1.id, true)
        }

        val service = AvtaleGjennomforingService(
            config = AvtaleGjennomforingService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            db = database.db,
            navAnsattService = mockk(),
        )

        val updateApentForPamelding = UpdateApentForPamelding(
            config = UpdateApentForPamelding.Config(disabled = true),
            db = database.db,
            avtaleGjennomforingService = service,
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("beholder påmelding når dato er før startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = startDato.minusDays(1))

            database.run {
                queries.gjennomforing.getAvtaleGjennomforingOrError(GjennomforingFixtures.GruppeFagYrke1.id).should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.getAvtaleGjennomforingOrError(GjennomforingFixtures.GruppeAmo1.id).should {
                    it.apentForPamelding shouldBe true
                }
            }
        }

        test("stenger påmelding for tiltak med felles oppstart når dato er lik startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now())

            database.run {
                queries.gjennomforing.getAvtaleGjennomforingOrError(GjennomforingFixtures.GruppeFagYrke1.id).should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.getAvtaleGjennomforingOrError(GjennomforingFixtures.GruppeAmo1.id).should {
                    it.apentForPamelding shouldBe false
                }
            }
        }

        test("beholder påmelding når dato er etter startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now().plusDays(1))

            database.run {
                queries.gjennomforing.getAvtaleGjennomforingOrError(GjennomforingFixtures.GruppeFagYrke1.id).should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.getAvtaleGjennomforingOrError(GjennomforingFixtures.GruppeAmo1.id).should {
                    it.apentForPamelding shouldBe true
                }
            }
        }
    }
})
