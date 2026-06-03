package no.nav.mulighetsrommet.api.avtale.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.tiltak.okonomi.Tilskuddstype

class PrismodellQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("AnnenAvtaltPris med totalbelop") {
        test("lagrer og henter totalbelop") {
            database.runAndRollback {
                val prismodell = PrismodellFixtures.createPrismodellDbo(
                    type = PrismodellType.ANNEN_AVTALT_PRIS,
                    tilsagnPerDeltaker = false,
                    totalbelop = 100_000u,
                )

                queries.prismodell.upsert(prismodell)

                queries.prismodell.getOrError(prismodell.id).shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().should {
                    it.totalbelop shouldBe 100_000u
                }

                queries.prismodell.upsert(prismodell.copy(totalbelop = null))

                queries.prismodell.getOrError(prismodell.id).shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().should {
                    it.totalbelop.shouldBeNull()
                }
            }
        }
    }

    context("TilskuddTilOpplaering") {
        test("lagrer og henter tilskudd") {
            database.runAndRollback {
                val prismodell = PrismodellFixtures.createPrismodellDbo(
                    type = PrismodellType.TILSKUDD_TIL_OPPLAERING,
                    tilskudd = mapOf(
                        Tilskuddstype.TILTAK_DRIFTSTILSKUDD to 50_000u,
                        Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD to 30_000u,
                    ),
                )

                queries.prismodell.upsert(prismodell)

                queries.prismodell.getOrError(prismodell.id).shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                    it.tilskudd shouldBe mapOf(
                        Tilskuddstype.TILTAK_DRIFTSTILSKUDD to 50_000u,
                        Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD to 30_000u,
                    )
                }
            }
        }

        test("tomt tilskudd-map lagres og hentes korrekt") {
            database.runAndRollback {
                val dbo = PrismodellFixtures.createPrismodellDbo(
                    type = PrismodellType.TILSKUDD_TIL_OPPLAERING,
                    tilskudd = emptyMap(),
                )
                queries.prismodell.upsert(dbo)

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                    it.tilskudd.shouldBeEmpty()
                }
            }
        }
    }
})
