package no.nav.mulighetsrommet.api.persistence.tiltak

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Valuta
import java.util.UUID

class PrismodellQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    context("AnskaffetEnkeltplass med totalbelop") {
        test("lagrer og henter totalbelop") {
            database.runAndRollback {
                val anskaffetEnkeltplass = Prismodell.AnskaffetEnkeltplass(
                    id = UUID.randomUUID(),
                    valuta = Valuta.NOK,
                    totalbelop = 100_000.NOK,
                )

                prismodell.upsert(anskaffetEnkeltplass)

                prismodell.getOrError(anskaffetEnkeltplass.id)
                    .shouldBeTypeOf<Prismodell.AnskaffetEnkeltplass>()
                    .totalbelop shouldBe 100_000.NOK

                prismodell.upsert(anskaffetEnkeltplass.copy(totalbelop = 999.NOK))

                prismodell.getOrError(anskaffetEnkeltplass.id)
                    .shouldBeTypeOf<Prismodell.AnskaffetEnkeltplass>()
                    .totalbelop shouldBe 999.NOK
            }
        }
    }

    context("TilskuddTilOpplaering") {
        test("lagrer og henter tilskudd") {
            database.runAndRollback {
                val tilskudd = Prismodell.TilskuddTilOpplaering(
                    id = UUID.randomUUID(),
                    valuta = Valuta.NOK,
                    tilleggsopplysninger = null,
                    tilskudd = mapOf(
                        Opplaeringtilskudd.Kode.SKOLEPENGER to 50_000.NOK,
                        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD to 30_000.NOK,
                    ),
                )

                prismodell.upsert(tilskudd)

                prismodell.getOrError(tilskudd.id).shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                    it.tilskudd shouldBe mapOf(
                        Opplaeringtilskudd.Kode.SKOLEPENGER to 50_000.NOK,
                        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD to 30_000.NOK,
                    )
                }
            }
        }

        test("tomt tilskudd-map lagres og hentes korrekt") {
            database.runAndRollback {
                val tilskudd = Prismodell.TilskuddTilOpplaering(
                    id = UUID.randomUUID(),
                    valuta = Valuta.NOK,
                    tilleggsopplysninger = null,
                    tilskudd = emptyMap(),
                )
                prismodell.upsert(tilskudd)

                prismodell.getOrError(tilskudd.id).shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                    it.tilskudd.shouldBeEmpty()
                }
            }
        }
    }

    context("IngenKostnader") {
        test("lagrer og henter aarsak") {
            database.runAndRollback {
                val ingenKostnader = Prismodell.IngenKostnader(
                    id = UUID.randomUUID(),
                    valuta = Valuta.NOK,
                    tilleggsopplysninger = null,
                    aarsak = Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI,
                )

                prismodell.upsert(ingenKostnader)

                prismodell.getOrError(ingenKostnader.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.aarsak shouldBe Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI
                }

                prismodell.upsert(ingenKostnader.copy(aarsak = Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT))

                prismodell.getOrError(ingenKostnader.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.aarsak shouldBe Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT
                }
            }
        }

        test("lagrer og henter tilleggsopplysninger") {
            database.runAndRollback {
                val ingenKostnader = Prismodell.IngenKostnader(
                    id = UUID.randomUUID(),
                    valuta = Valuta.NOK,
                    tilleggsopplysninger = "Finansiert av arbeidsgiver",
                    aarsak = Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI,
                )

                prismodell.upsert(ingenKostnader)

                prismodell.getOrError(ingenKostnader.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.tilleggsopplysninger shouldBe "Finansiert av arbeidsgiver"
                }

                prismodell.upsert(ingenKostnader.copy(tilleggsopplysninger = null))

                prismodell.getOrError(ingenKostnader.id).shouldBeTypeOf<Prismodell.IngenKostnader>().should {
                    it.tilleggsopplysninger.shouldBeNull()
                }
            }
        }
    }
})
