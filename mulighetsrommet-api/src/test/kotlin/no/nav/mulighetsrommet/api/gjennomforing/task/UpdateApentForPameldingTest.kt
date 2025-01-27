package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import java.time.LocalDate

class UpdateApentForPameldingTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("steng påmelding for tiltak med felles oppstart") {
        val startDato = LocalDate.now()
        val sluttDato = LocalDate.now().plusMonths(1)

        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Jobbklubb, TiltakstypeFixtures.GruppeAmo),
            avtaler = listOf(AvtaleFixtures.jobbklubb, AvtaleFixtures.gruppeAmo),
            gjennomforinger = listOf(
                GjennomforingFixtures.Jobbklubb1.copy(
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
            queries.gjennomforing.setApentForPamelding(GjennomforingFixtures.Jobbklubb1.id, true)
            queries.gjennomforing.setApentForPamelding(GjennomforingFixtures.GruppeAmo1.id, true)
        }

        val service = GjennomforingService(
            db = database.db,
            gjennomforingKafkaProducer = mockk(relaxed = true),
            validator = mockk(),
            navAnsattService = mockk(relaxed = true),
        )

        val updateApentForPamelding = UpdateApentForPamelding(
            config = UpdateApentForPamelding.Config(disabled = true),
            db = database.db,
            gjennomforingService = service,
        )

        beforeEach {
            domain.initialize(database.db)
        }

        test("beholder påmelding når dato er før startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = startDato.minusDays(1))

            database.run {
                queries.gjennomforing.get(GjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.get(GjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
            }
        }

        test("stenger påmelding for tiltak med felles oppstart når dato er lik startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now())

            database.run {
                queries.gjennomforing.get(GjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.get(GjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe false
                }
            }
        }

        test("beholder påmelding når dato er etter startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now().plusDays(1))

            database.run {
                queries.gjennomforing.get(GjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
                queries.gjennomforing.get(GjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
            }
        }
    }
})
