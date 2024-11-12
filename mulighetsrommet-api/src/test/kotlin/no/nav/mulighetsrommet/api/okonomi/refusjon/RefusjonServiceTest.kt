package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.okonomi.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.okonomi.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.okonomi.refusjon.model.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.okonomi.refusjon.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.refusjon.model.DeltakelsePerioder
import no.nav.mulighetsrommet.api.okonomi.refusjon.model.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.Kid
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class RefusjonServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("generering av refusjonskrav for AFT") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val refusjonskravRepository = RefusjonskravRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)

        val service = RefusjonService(
            tiltaksgjennomforingRepository = tiltaksgjennomforingRepository,
            deltakerRepository = deltakerRepository,
            refusjonskravRepository = refusjonskravRepository,
            db = database.db,
        )

        fun getOrgnrForArrangor(
            gjennomforing: TiltaksgjennomforingDbo,
            domain: MulighetsrommetTestDomain,
        ): Organisasjonsnummer {
            return requireNotNull(domain.arrangorer.find { it.id == gjennomforing.arrangorId }?.organisasjonsnummer)
        }

        test("genererer et refusjonskrav med riktig periode, frist og sats som input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
            )
            domain.initialize(database.db)

            service.genererRefusjonskravForMonth(LocalDate.of(2024, 1, 1))

            val allKrav = refusjonskravRepository.getByArrangorIds(
                getOrgnrForArrangor(
                    AFT1,
                    domain,
                ),
            )
            allKrav.size shouldBe 1

            val krav = allKrav.first()
            krav.gjennomforing.id shouldBe AFT1.id
            krav.fristForGodkjenning shouldBe LocalDateTime.of(2024, 4, 1, 0, 0, 0)
            krav.beregning.input shouldBe RefusjonKravBeregningAft.Input(
                periodeStart = LocalDate.of(2024, 1, 1).atStartOfDay(),
                periodeSlutt = LocalDate.of(2024, 2, 1).atStartOfDay(),
                sats = 20205,
                deltakelser = setOf(),
            )
        }

        test("genererer et refusjonskrav med kontonummer og kid-nummer fra forrige godkjente refusjonskrav fra arrangør") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
            )
            domain.initialize(database.db)

            val januar = LocalDate.of(2024, 1, 1)
            service.genererRefusjonskravForMonth(januar)

            val allKrav = refusjonskravRepository.getByArrangorIds(
                getOrgnrForArrangor(
                    AFT1,
                    domain,
                ),
            )
            allKrav.size shouldBe 1

            val krav = allKrav.first()
            krav.gjennomforing.id shouldBe AFT1.id
            krav.fristForGodkjenning shouldBe LocalDateTime.of(2024, 4, 1, 0, 0, 0)
            krav.betalingsinformasjon.kontonummer shouldBe null
            krav.betalingsinformasjon.kid shouldBe null
            krav.beregning.input shouldBe RefusjonKravBeregningAft.Input(
                periodeStart = LocalDate.of(2024, 1, 1).atStartOfDay(),
                periodeSlutt = LocalDate.of(2024, 2, 1).atStartOfDay(),
                sats = 20205,
                deltakelser = setOf(),
            )
            refusjonskravRepository.setBetalingsInformasjon(
                id = krav.id,
                kontonummer = Kontonummer("12345678901"),
                kid = Kid("12345678901"),
            )
            refusjonskravRepository.setGodkjentAvArrangor(krav.id, LocalDateTime.now())

            val februar = LocalDate.of(2024, 2, 1)
            service.genererRefusjonskravForMonth(februar)
            val nyeKrav = refusjonskravRepository.getByArrangorIds(
                getOrgnrForArrangor(
                    AFT1,
                    domain,
                ),
            )
            nyeKrav.size shouldBe 2

            val sisteKrav = nyeKrav.first()
            sisteKrav.gjennomforing.id shouldBe AFT1.id
            sisteKrav.fristForGodkjenning shouldBe LocalDateTime.of(2024, 5, 1, 0, 0, 0)
            sisteKrav.betalingsinformasjon.kontonummer shouldBe Kontonummer("12345678901")
            sisteKrav.betalingsinformasjon.kid shouldBe Kid("12345678901")
            sisteKrav.beregning.input shouldBe RefusjonKravBeregningAft.Input(
                periodeStart = LocalDate.of(2024, 2, 1).atStartOfDay(),
                periodeSlutt = LocalDate.of(2024, 3, 1).atStartOfDay(),
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
                        statusType = DeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 15),
                        statusType = DeltakerStatus.Type.DELTAR,
                        stillingsprosent = 40.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        stillingsprosent = 50.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2023, 12, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = DeltakerStatus.Type.IKKE_AKTUELL,
                        stillingsprosent = 100.0,
                    ),
                ),
            )
            domain.initialize(database.db)

            service.genererRefusjonskravForMonth(LocalDate.of(2024, 1, 1))

            val krav = refusjonskravRepository.getByArrangorIds(
                getOrgnrForArrangor(
                    AFT1,
                    domain,
                ),
            ).first()

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
                        statusType = DeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                ),
            )
            domain.initialize(database.db)

            service.genererRefusjonskravForMonth(LocalDate.of(2024, 1, 1))

            val krav = refusjonskravRepository.getByArrangorIds(
                getOrgnrForArrangor(
                    AFT1,
                    domain,
                ),
            ).first()

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

        test("deltaker med startDato lik periodeSlutt blir ikke med i kravet") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 6, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2024, 2, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        stillingsprosent = 100.0,
                    ),
                ),
            )
            domain.initialize(database.db)
            service.genererRefusjonskravForMonth(LocalDate.of(2024, 1, 1))

            val krav = refusjonskravRepository.getByArrangorIds(
                getOrgnrForArrangor(
                    AFT1,
                    domain,
                ),
            ).first()

            krav.beregning.input.shouldBeTypeOf<RefusjonKravBeregningAft.Input>().should {
                it.deltakelser shouldHaveSize 1
            }
        }
    }

    context("rekalkulering av refusjonskrav for AFT") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val refusjonskravRepository = RefusjonskravRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)

        val service = RefusjonService(
            tiltaksgjennomforingRepository = tiltaksgjennomforingRepository,
            deltakerRepository = deltakerRepository,
            refusjonskravRepository = refusjonskravRepository,
            db = database.db,
        )

        val domain = MulighetsrommetTestDomain(
            gjennomforinger = listOf(AFT1),
            deltakere = listOf(
                DeltakerFixtures.createDeltaker(
                    AFT1.id,
                    startDato = LocalDate.of(2024, 6, 1),
                    sluttDato = LocalDate.of(2024, 6, 30),
                    statusType = DeltakerStatus.Type.DELTAR,
                    stillingsprosent = 100.0,
                ),
            ),
        )

        lateinit var krav1: RefusjonskravDbo

        beforeEach {
            domain.initialize(database.db)

            krav1 = service.createRefusjonskravAft(
                refusjonskravId = UUID.randomUUID(),
                gjennomforingId = AFT1.id,
                periodeStart = LocalDate.of(2024, 6, 1).atStartOfDay(),
                periodeSlutt = LocalDate.of(2024, 7, 1).atStartOfDay(),
            )
            refusjonskravRepository.upsert(krav1)

            val krav2 = refusjonskravRepository.get(krav1.id).shouldNotBeNull()
            krav2.beregning.output.shouldBeTypeOf<RefusjonKravBeregningAft.Output>().should {
                it.belop shouldBe 20205
                it.deltakelser shouldBe setOf(
                    DeltakelseManedsverk(
                        deltakelseId = domain.deltakere[0].id,
                        manedsverk = 1.0,
                    ),
                )
            }
        }

        test("oppdaterer beregnet refusjonskrav når deltakelser endres") {
            deltakerRepository.upsert(
                domain.deltakere[0].copy(
                    sluttDato = LocalDate.of(2024, 6, 15),
                ),
            )
            service.recalculateRefusjonskravForGjennomforing(AFT1.id)

            val krav2 = refusjonskravRepository.get(krav1.id).shouldNotBeNull()
            krav2.beregning.output.shouldBeTypeOf<RefusjonKravBeregningAft.Output>().should {
                it.belop shouldBe 10102
                it.deltakelser shouldBe setOf(
                    DeltakelseManedsverk(
                        deltakelseId = domain.deltakere[0].id,
                        manedsverk = 0.5,
                    ),
                )
            }
        }

        test("oppdaterer ikke refusjonskrav hvis det allerede er godkjent av arrangør") {
            deltakerRepository.upsert(
                domain.deltakere[0].copy(
                    sluttDato = LocalDate.of(2024, 6, 15),
                ),
            )
            refusjonskravRepository.setGodkjentAvArrangor(krav1.id, LocalDateTime.now())
            service.recalculateRefusjonskravForGjennomforing(AFT1.id)

            val krav2 = refusjonskravRepository.get(krav1.id).shouldNotBeNull()
            krav2.beregning shouldBe krav1.beregning
        }
    }
})
