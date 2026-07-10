package no.nav.mulighetsrommet.api.avtale.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener

class PrismodellQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    context("AnnenAvtaltPris med totalbelop") {
        test("lagrer og henter totalbelop") {
            database.runAndRollback {
                val dbo = PrismodellFixtures.createPrismodellDbo(
                    type = PrismodellType.ANNEN_AVTALT_PRIS,
                    tilsagnPerDeltaker = false,
                    totalbelop = 100_000,
                )

                queries.prismodell.upsert(dbo)

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().should {
                    it.totalbelop shouldBe 100_000
                }

                queries.prismodell.upsert(dbo.copy(totalbelop = null))

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().should {
                    it.totalbelop.shouldBeNull()
                }
            }
        }
    }

    context("TilskuddTilOpplaering") {
        test("lagrer og henter tilskudd") {
            database.runAndRollback {
                val dbo = PrismodellFixtures.createPrismodellDbo(
                    type = PrismodellType.TILSKUDD_TIL_OPPLAERING,
                    tilskudd = mapOf(
                        Opplaeringtilskudd.Kode.SKOLEPENGER to 50_000,
                        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD to 30_000,
                    ),
                )

                queries.prismodell.upsert(dbo)

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                    it.tilskudd shouldBe mapOf(
                        Opplaeringtilskudd.Kode.SKOLEPENGER to 50_000,
                        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD to 30_000,
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

    context("IngenKostnader") {
        test("lagrer og henter aarsak") {
            database.runAndRollback {
                val dbo = PrismodellFixtures.createPrismodellDbo(
                    type = PrismodellType.INGEN_KOSTNADER,
                    aarsak = Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI.name,
                )

                queries.prismodell.upsert(dbo)

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.aarsak shouldBe Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI
                }

                queries.prismodell.upsert(dbo.copy(aarsak = Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT.name))

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.aarsak shouldBe Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT
                }
            }
        }

        test("lagrer og henter tilleggsopplysninger") {
            database.runAndRollback {
                val dbo = PrismodellFixtures.createPrismodellDbo(
                    type = PrismodellType.INGEN_KOSTNADER,
                    aarsak = Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT.name,
                    prisbetingelser = "Finansiert av arbeidsgiver",
                )

                queries.prismodell.upsert(dbo)

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.tilleggsopplysninger shouldBe "Finansiert av arbeidsgiver"
                }

                queries.prismodell.upsert(dbo.copy(prisbetingelser = null))

                queries.prismodell.getOrError(dbo.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.tilleggsopplysninger.shouldBeNull()
                }
            }
        }
    }
})
