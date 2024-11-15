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
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.AvvistTilsagnAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBesluttelseStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
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
            opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            arrangorId = ArrangorFixtures.underenhet1.id,
            beregning = Prismodell.TilsagnBeregning.Fri(123),
        )

        test("upsert and get") {
            repository.upsert(tilsagn)
            repository.get(tilsagn.id) shouldBe TilsagnDto(
                id = tilsagn.id,
                tiltaksgjennomforing = TilsagnDto.Tiltaksgjennomforing(
                    id = AFT1.id,
                    antallPlasser = AFT1.antallPlasser,
                ),
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                kostnadssted = Gjovik,
                besluttelse = null,
                annullertTidspunkt = null,
                lopenummer = 1,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                arrangor = TilsagnDto.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                ),
                beregning = Prismodell.TilsagnBeregning.Fri(123),
            )
        }

        test("besluttelse set and get") {
            repository.upsert(tilsagn)
            repository.setBesluttelse(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(AvvistTilsagnAarsak.FEIL_ANNET),
                    forklaring = "Forklaring",
                ),
                navIdent = NavIdent("Z123456"),
                tidspunkt = LocalDateTime.of(2023, 2, 2, 0, 0, 0),
            )

            repository.get(tilsagn.id)?.besluttelse shouldBe TilsagnDto.Besluttelse(
                navIdent = NavIdent("Z123456"),
                tidspunkt = LocalDateTime.of(2023, 2, 2, 0, 0, 0),
                status = TilsagnBesluttelseStatus.AVVIST,
                aarsaker = listOf(AvvistTilsagnAarsak.FEIL_ANNET),
                forklaring = "Forklaring",
            )
        }

        test("upsert nuller ut besluttelse") {
            repository.upsert(tilsagn)
            repository.setBesluttelse(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(AvvistTilsagnAarsak.FEIL_ANNET),
                    forklaring = "Forklaring",
                ),
                navIdent = NavIdent("Z123456"),
                tidspunkt = LocalDateTime.of(2023, 2, 2, 0, 0, 0),
            )
            repository.upsert(tilsagn)
            repository.get(tilsagn.id)?.besluttelse shouldBe null
        }

        test("get by arrangor_ids") {
            repository.upsert(tilsagn)
            repository.setBesluttelse(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavIdent("Z123456"),
                tidspunkt = LocalDateTime.now(),
            )
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
            repository.setBesluttelse(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavIdent("Z123456"),
                tidspunkt = LocalDateTime.now(),
            )
            repository.getArrangorflateTilsagnTilRefusjon(
                tilsagn.tiltaksgjennomforingId,
                LocalDate.of(2023, 1, 14),
                LocalDate.of(2023, 1, 15),
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