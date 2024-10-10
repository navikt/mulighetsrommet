package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePerioder
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import java.time.LocalDate

class RefusjonServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("Generering av refusjonskrav for AFT") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val refusjonskravRepository = RefusjonskravRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)

        val service = RefusjonService(
            tiltaksgjennomforingRepository = tiltaksgjennomforingRepository,
            deltakerRepository = deltakerRepository,
            refusjonskravRepository = refusjonskravRepository,
            db = database.db,
        )

        test("genererer et refusjonskrav med riktig periode og sats som input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
            )
            domain.initialize(database.db)

            service.genererRefusjonskravForMonth(LocalDate.of(2024, 1, 1))

            val allKrav = service.getByArrangorIds(
                listOf(ArrangorFixtures.underenhet1.id),
            )
            allKrav.size shouldBe 1

            val krav = allKrav.first()
            krav.gjennomforing.id shouldBe AFT1.id
            krav.beregning.input shouldBe RefusjonKravBeregningAft.Input(
                periodeStart = LocalDate.of(2024, 1, 1).atStartOfDay(),
                periodeSlutt = LocalDate.of(2024, 2, 1).atStartOfDay(),
                sats = 20205,
                deltakelser = setOf(),
            )
        }

        test("genererer et refusjonskrav med relevante deltakelser som input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = AmtDeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 15),
                        statusType = AmtDeltakerStatus.Type.DELTAR,
                        stillingsprosent = 40.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        statusType = AmtDeltakerStatus.Type.DELTAR,
                        stillingsprosent = 50.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2023, 12, 31),
                        statusType = AmtDeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = AmtDeltakerStatus.Type.IKKE_AKTUELL,
                        stillingsprosent = 100.0,
                    ),
                ),
            )
            domain.initialize(database.db)

            service.genererRefusjonskravForMonth(LocalDate.of(2024, 1, 1))

            val krav = service
                .getByArrangorIds(listOf(ArrangorFixtures.underenhet1.id))
                .first()

            krav.beregning.input.shouldBeTypeOf<RefusjonKravBeregningAft.Input>().should {
                it.deltakelser shouldBe setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1).atStartOfDay(),
                                slutt = LocalDate.of(2024, 2, 1).atStartOfDay(),
                                stillingsprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[1].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1).atStartOfDay(),
                                slutt = LocalDate.of(2024, 1, 16).atStartOfDay(),
                                stillingsprosent = 40.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[2].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1).atStartOfDay(),
                                slutt = LocalDate.of(2024, 2, 1).atStartOfDay(),
                                stillingsprosent = 50.0,
                            ),
                        ),
                    ),
                )
            }
        }

        test("genererer et refusjonskrav med beregnet belop basert på input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = AmtDeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                ),
            )
            domain.initialize(database.db)

            service.genererRefusjonskravForMonth(LocalDate.of(2024, 1, 1))

            val krav = service
                .getByArrangorIds(listOf(ArrangorFixtures.underenhet1.id))
                .first()

            krav.beregning.output.shouldBeTypeOf<RefusjonKravBeregningAft.Output>().should {
                it.belop shouldBe 20205
                it.deltakelser shouldBe setOf(
                    DeltakelseManedsverk(
                        deltakelseId = domain.deltakere[0].id,
                        manedsverk = 1.0,
                    ),
                )
            }
        }
    }
})
