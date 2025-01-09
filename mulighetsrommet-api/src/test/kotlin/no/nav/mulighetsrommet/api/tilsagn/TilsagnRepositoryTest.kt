package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TilsagnRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(AFT1),
    )

    beforeEach {
        database.truncateAll()
        domain.initialize(database.db)
    }

    val tilsagn = TilsagnDbo(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = AFT1.id,
        periodeStart = LocalDate.of(2023, 1, 1),
        periodeSlutt = LocalDate.of(2023, 2, 1),
        kostnadssted = Gjovik.enhetsnummer,
        arrangorId = ArrangorFixtures.underenhet1.id,
        beregning = TilsagnBeregningFri(TilsagnBeregningFri.Input(123), TilsagnBeregningFri.Output(123)),
        endretAv = NavAnsattFixture.ansatt1.navIdent,
        endretTidspunkt = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
        type = TilsagnType.TILSAGN,
    )

    context("CRUD") {
        val repository = TilsagnRepository(database.db)

        test("upsert and get") {
            repository.upsert(tilsagn)
            repository.get(tilsagn.id) shouldBe TilsagnDto(
                id = tilsagn.id,
                tiltaksgjennomforing = TilsagnDto.Tiltaksgjennomforing(
                    id = AFT1.id,
                ),
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                kostnadssted = Gjovik,
                lopenummer = 1,
                arrangor = TilsagnDto.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                ),
                beregning = TilsagnBeregningFri(TilsagnBeregningFri.Input(123), TilsagnBeregningFri.Output(123)),
                status = TilsagnDto.TilsagnStatus.TilGodkjenning(
                    endretAv = NavAnsattFixture.ansatt1.navIdent,
                    endretTidspunkt = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                ),
                type = TilsagnType.TILSAGN,
            )
        }

        test("get all") {
            repository.upsert(tilsagn)

            repository.getAll(statuser = listOf(TilsagnStatus.TIL_GODKJENNING)).shouldHaveSize(1)
            repository.getAll(statuser = listOf(TilsagnStatus.TIL_ANNULLERING)).shouldHaveSize(0)
            repository.getAll(statuser = listOf(TilsagnStatus.ANNULLERT)).shouldHaveSize(0)

            repository.getAll(gjennomforingId = AFT1.id).shouldHaveSize(1)
            repository.getAll(gjennomforingId = UUID.randomUUID()).shouldHaveSize(0)

            repository.getAll(type = TilsagnType.TILSAGN).shouldHaveSize(1)
            repository.getAll(type = TilsagnType.EKSTRATILSAGN).shouldHaveSize(0)
        }

        test("delete") {
            repository.upsert(tilsagn)
            repository.delete(tilsagn.id)
            repository.get(tilsagn.id) shouldBe null
        }
    }

    context("endre status på tilsagn") {
        val repository = TilsagnRepository(database.db)

        test("annuller") {
            repository.upsert(tilsagn)
            val endretTidspunkt = LocalDateTime.now()

            // Send til annullering
            database.db.transaction { tx ->
                repository.tilAnnullering(
                    tilsagn.id,
                    tilsagn.endretAv,
                    endretTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                    tx,
                )
            }
            repository.get(tilsagn.id)?.status should {
                when (it) {
                    is TilsagnDto.TilsagnStatus.TilAnnullering -> {
                        it.endretAv shouldBe tilsagn.endretAv
                        it.endretAvNavn shouldBe "${NavAnsattFixture.ansatt1.fornavn} ${NavAnsattFixture.ansatt1.etternavn}"
                        it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        it.forklaring shouldBe "Min forklaring"
                    }

                    else -> throw Exception("Feil status")
                }
            }

            // Beslutt annullering
            database.db.transaction { tx ->
                repository.besluttAnnullering(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                    tx,
                )
            }
            repository.get(tilsagn.id)?.status should {
                when (it) {
                    is TilsagnDto.TilsagnStatus.Annullert -> {
                        it.endretAv shouldBe tilsagn.endretAv
                        it.godkjentAv shouldBe NavIdent("B123456")
                        it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        it.forklaring shouldBe "Min forklaring"
                    }

                    else -> throw Exception("Feil status")
                }
            }
        }

        test("avbryt annullering") {
            repository.upsert(tilsagn)
            val endretTidspunkt = LocalDateTime.now()

            // Send til annullering
            database.db.transaction { tx ->
                repository.tilAnnullering(
                    tilsagn.id,
                    tilsagn.endretAv,
                    endretTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                    tx,
                )
                // Avbryt annullering
                repository.avbrytAnnullering(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                    tx,
                )
            }
            repository.get(tilsagn.id)?.status shouldBe TilsagnDto.TilsagnStatus.Godkjent
        }

        test("godkjenn") {
            val besluttetTidspunkt = LocalDateTime.of(2024, 12, 12, 0, 0)
            repository.upsert(tilsagn)
            database.db.transaction { tx ->
                repository.besluttGodkjennelse(
                    tilsagn.id,
                    NavIdent("B123456"),
                    besluttetTidspunkt,
                    tx,
                )
            }
            repository.get(tilsagn.id)?.status shouldBe TilsagnDto.TilsagnStatus.Godkjent
        }

        test("returner") {
            val returnertTidspunkt = LocalDateTime.of(2024, 12, 12, 0, 0)
            repository.upsert(tilsagn)
            database.db.transaction { tx ->
                repository.returner(
                    tilsagn.id,
                    NavAnsattFixture.ansatt2.navIdent,
                    returnertTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                    tx,
                )
            }
            repository.get(tilsagn.id)?.status should {
                when (it) {
                    is TilsagnDto.TilsagnStatus.Returnert -> {
                        it.endretAv shouldBe tilsagn.endretAv
                        it.returnertAvNavn shouldBe "${NavAnsattFixture.ansatt2.fornavn} ${NavAnsattFixture.ansatt2.etternavn}"
                        it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        it.forklaring shouldBe "Min forklaring"
                    }

                    else -> throw Exception("Feil status")
                }
            }
        }

        test("Skal få status TIL_GODKJENNING etter upsert") {
            repository.upsert(tilsagn)
            repository.get(tilsagn.id)?.status should {
                when (it) {
                    is TilsagnDto.TilsagnStatus.TilGodkjenning -> {
                        it.endretAv shouldBe tilsagn.endretAv
                    }

                    else -> throw Exception("Feil status")
                }
            }
        }
    }

    context("tilsagn for arrangører") {
        val repository = TilsagnRepository(database.db)

        test("get by arrangor_ids") {
            repository.upsert(tilsagn)
            database.db.transaction { tx ->
                repository.besluttGodkjennelse(
                    tilsagn.id,
                    NavIdent("B123456"),
                    LocalDateTime.now(),
                    tx,
                )
            }
            repository.getAllArrangorflateTilsagn(domain.arrangorer.find { it.id == tilsagn.arrangorId }?.organisasjonsnummer!!) shouldBe listOf(
                ArrangorflateTilsagn(
                    id = tilsagn.id,
                    gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                        navn = AFT1.navn,
                    ),
                    tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                        navn = TiltakstypeFixtures.AFT.navn,
                    ),
                    periodeStart = LocalDate.of(2023, 1, 1),
                    periodeSlutt = LocalDate.of(2023, 2, 1),
                    arrangor = ArrangorflateTilsagn.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    ),
                    beregning = TilsagnBeregningFri(TilsagnBeregningFri.Input(123), TilsagnBeregningFri.Output(123)),
                ),
            )
            repository.getArrangorflateTilsagn(tilsagn.id)?.id shouldBe tilsagn.id
        }

        test("get til refusjon") {
            repository.upsert(tilsagn)
            database.db.transaction { tx ->
                repository.besluttGodkjennelse(
                    tilsagn.id,
                    NavIdent("B123456"),
                    LocalDateTime.now(),
                    tx,
                )
            }
            repository.getArrangorflateTilsagnTilRefusjon(
                tilsagn.tiltaksgjennomforingId,
                RefusjonskravPeriode.fromDayInMonth(LocalDate.of(2023, 1, 1)),
            ) shouldBe listOf(
                ArrangorflateTilsagn(
                    id = tilsagn.id,
                    gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                        navn = AFT1.navn,
                    ),
                    tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                        navn = TiltakstypeFixtures.AFT.navn,
                    ),
                    periodeStart = LocalDate.of(2023, 1, 1),
                    periodeSlutt = LocalDate.of(2023, 2, 1),
                    arrangor = ArrangorflateTilsagn.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    ),
                    beregning = TilsagnBeregningFri(TilsagnBeregningFri.Input(123), TilsagnBeregningFri.Output(123)),
                ),
            )
        }
    }
})
