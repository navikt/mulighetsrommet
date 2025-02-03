package no.nav.mulighetsrommet.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.tiltak.okonomi.OkonomiPart
import no.nav.mulighetsrommet.tiltak.okonomi.databaseConfig
import no.nav.mulighetsrommet.tiltak.okonomi.db.BestillingDbo
import no.nav.mulighetsrommet.tiltak.okonomi.db.FakturaDbo
import no.nav.mulighetsrommet.tiltak.okonomi.db.LinjeDbo
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.Kilde
import java.time.LocalDate

class OebsFakturaQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val bestillingDbo = BestillingDbo(
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arrangorHovedenhet = Organisasjonsnummer("123456789"),
        arrangorUnderenhet = Organisasjonsnummer("234567890"),
        kostnadssted = NavEnhetNummer("0400"),
        bestillingsnummer = "A-1",
        avtalenummer = null,
        belop = 1000,
        periode = Periode(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 3, 1),
        ),
        opprettetAv = OkonomiPart.System(Kilde.TILTADM),
        opprettetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
        besluttetTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        annullert = false,
        linjer = listOf(
            LinjeDbo(
                linjenummer = 1,
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 2, 1),
                ),
                belop = 500,
            ),
            LinjeDbo(
                linjenummer = 2,
                periode = Periode(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 3, 1),
                ),
                belop = 500,
            ),
        ),
    )

    test("opprett faktura") {
        val fakturaDbo = FakturaDbo(
            fakturanummer = "4567",
            bestillingsnummer = "A-1",
            kontonummer = Kontonummer("12345678901"),
            kid = Kid("123123123123123"),
            belop = 500,
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            opprettetAv = OkonomiPart.System(Kilde.TILTADM),
            opprettetTidspunkt = LocalDate.of(2025, 2, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            besluttetTidspunkt = LocalDate.of(2025, 2, 2).atStartOfDay(),
            linjer = listOf(
                LinjeDbo(
                    linjenummer = 1,
                    periode = Periode(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 2, 1),
                    ),
                    belop = 500,
                ),
                LinjeDbo(
                    linjenummer = 2,
                    periode = Periode(
                        LocalDate.of(2025, 2, 1),
                        LocalDate.of(2025, 3, 1),
                    ),
                    belop = 500,
                ),
            ),
        )

        database.runAndRollback {
            val bestilling = BestillingQueries(it)
            bestilling.createBestilling(bestillingDbo)

            val faktura = FakturaQueries(it)

            faktura.opprettFaktura(fakturaDbo)

            faktura.getFaktura("4567") shouldBe fakturaDbo
        }
    }
})
