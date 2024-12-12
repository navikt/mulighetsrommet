package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TilsagnRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(
            AFT1,
        ),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    context("CRUD") {
        val repository = TilsagnRepository(database.db)

        val tilsagn = TilsagnDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = AFT1.id,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            arrangorId = ArrangorFixtures.underenhet1.id,
            beregning = Prismodell.TilsagnBeregning.Fri(123),
            endretAv = NavAnsattFixture.ansatt1.navIdent,
            endretTidspunkt = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
        )

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
                beregning = Prismodell.TilsagnBeregning.Fri(123),
                status = TilsagnDto.TilsagnStatus.TilGodkjenning(
                    endretAv = NavAnsattFixture.ansatt1.navIdent,
                    endretTidspunkt = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                ),
            )
        }

        test("annuller") {
            repository.upsert(tilsagn)
            val endretTidspunkt = LocalDateTime.now()

            // Send til annullering
            repository.tilAnnullering(
                tilsagn.id,
                tilsagn.endretAv,
                endretTidspunkt,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                forklaring = "Min forklaring",
            )
            repository.get(tilsagn.id)?.status shouldBe TilsagnDto.TilsagnStatus.TilAnnullering(
                endretTidspunkt = endretTidspunkt,
                endretAv = tilsagn.endretAv,
                endretAvNavn = "${NavAnsattFixture.ansatt1.fornavn} ${NavAnsattFixture.ansatt1.etternavn}",
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                forklaring = "Min forklaring",
            )

            // Beslutt annullering
            repository.besluttAnnullering(
                tilsagn.id,
                NavIdent("B123456"),
                endretTidspunkt,
            )
            repository.get(tilsagn.id)?.status shouldBe TilsagnDto.TilsagnStatus.Annullert(
                endretTidspunkt = endretTidspunkt,
                endretAv = tilsagn.endretAv,
                godkjentAv = NavIdent("B123456"),
            )
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
            repository.returner(
                tilsagn.id,
                NavAnsattFixture.ansatt2.navIdent,
                returnertTidspunkt,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                forklaring = "Min forklaring",
            )
            repository.get(tilsagn.id)?.status shouldBe TilsagnDto.TilsagnStatus.Returnert(
                endretTidspunkt = returnertTidspunkt,
                endretAv = tilsagn.endretAv,
                returnertAv = NavAnsattFixture.ansatt2.navIdent,
                returnertAvNavn = "${NavAnsattFixture.ansatt2.fornavn} ${NavAnsattFixture.ansatt2.etternavn}",
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                forklaring = "Min forklaring",
            )
        }

        test("Skal få status TIL_GODKJENNING etter upsert") {
            repository.upsert(tilsagn)
            repository.get(tilsagn.id)?.status shouldBe TilsagnDto.TilsagnStatus.TilGodkjenning(
                endretTidspunkt = tilsagn.endretTidspunkt,
                endretAv = tilsagn.endretAv,
            )
        }

        test("delete") {
            repository.upsert(tilsagn)
            repository.delete(tilsagn.id)
            repository.get(tilsagn.id) shouldBe null
        }

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
                    beregning = Prismodell.TilsagnBeregning.Fri(123),
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
                    beregning = Prismodell.TilsagnBeregning.Fri(123),
                ),
            )
        }
    }
})
