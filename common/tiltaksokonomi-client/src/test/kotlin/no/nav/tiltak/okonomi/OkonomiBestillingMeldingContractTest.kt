package no.nav.tiltak.okonomi

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.Instant
import java.time.LocalDate

class OkonomiBestillingMeldingContractTest : FunSpec({

    test("BESTILLING med norsk arrangør og behandlet av system, besluttet av nav-ansatt") {
        assertContract<OkonomiBestillingMelding>(
            OkonomiBestillingMelding.Bestilling(
                payload = OpprettBestilling(
                    bestillingsnummer = Bestillingsnummer("A-1-1"),
                    tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                    arrangor = OpprettBestilling.Arrangor.Norsk(
                        organisasjonsnummer = Organisasjonsnummer("123456789"),
                    ),
                    kostnadssted = NavEnhetNummer("0400"),
                    avtalenummer = "avtale-1",
                    belop = 1000,
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    behandletAv = OkonomiPart.System(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                    besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                    valuta = Valuta.NOK,
                ),
            ),
        )
    }

    test("BESTILLING med utenlandsk arrangør") {
        assertContract<OkonomiBestillingMelding>(
            OkonomiBestillingMelding.Bestilling(
                payload = OpprettBestilling(
                    bestillingsnummer = Bestillingsnummer("E-1-1"),
                    tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                    arrangor = OpprettBestilling.Arrangor.Utenlandsk(
                        organisasjonsnummer = Organisasjonsnummer("123456789"),
                        navn = "Utenlandsk Arrangør AS",
                        gateNavn = "Main Street 1",
                        by = "Stockholm",
                        postNummer = "12345",
                        landKode = "SE",
                    ),
                    kostnadssted = NavEnhetNummer("0400"),
                    avtalenummer = null,
                    belop = 2000,
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    behandletAv = OkonomiPart.System(OkonomiFagsystem.EKSPERTBISTAND),
                    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    besluttetAv = OkonomiPart.System(OkonomiFagsystem.EKSPERTBISTAND),
                    besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                    valuta = Valuta.NOK,
                ),
            ),
        )
    }

    test("ANNULLERING") {
        assertContract<OkonomiBestillingMelding>(
            OkonomiBestillingMelding.Annullering(
                payload = AnnullerBestilling(
                    bestillingsnummer = Bestillingsnummer("A-1-1"),
                    behandletAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                    behandletTidspunkt = Instant.parse("2025-01-03T00:00:00Z"),
                    besluttetAv = OkonomiPart.System(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                    besluttetTidspunkt = Instant.parse("2025-01-04T00:00:00Z"),
                ),
            ),
        )
    }

    test("FAKTURA med BBan-betalingsinformasjon") {
        assertContract<OkonomiBestillingMelding>(
            OkonomiBestillingMelding.Faktura(
                payload = OpprettFaktura(
                    fakturanummer = Fakturanummer("A-1-1-1"),
                    bestillingsnummer = Bestillingsnummer("A-1-1"),
                    betalingsinformasjon = OpprettFaktura.Betalingsinformasjon.BBan(
                        kontonummer = Kontonummer("12345678901"),
                        kid = Kid.parseOrThrow("0004614992"),
                    ),
                    belop = 1000,
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    behandletAv = OkonomiPart.System(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                    besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                    gjorOppBestilling = false,
                    beskrivelse = "En beskrivelse",
                    valuta = Valuta.NOK,
                ),
            ),
        )
    }

    test("FAKTURA med IBan-betalingsinformasjon") {
        assertContract<OkonomiBestillingMelding>(
            OkonomiBestillingMelding.Faktura(
                payload = OpprettFaktura(
                    fakturanummer = Fakturanummer("A-1-1-1"),
                    bestillingsnummer = Bestillingsnummer("A-1-1"),
                    betalingsinformasjon = OpprettFaktura.Betalingsinformasjon.IBan(
                        bic = "DABANO22",
                        iban = "NO9386011117947",
                        bankNavn = "DNB",
                        bankLandKode = "NO",
                    ),
                    belop = 1000,
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    behandletAv = OkonomiPart.System(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                    besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                    gjorOppBestilling = true,
                    beskrivelse = null,
                    valuta = Valuta.NOK,
                ),
            ),
        )
    }

    test("GJOR_OPP_BESTILLING") {
        assertContract<OkonomiBestillingMelding>(
            OkonomiBestillingMelding.GjorOppBestilling(
                payload = GjorOppBestilling(
                    bestillingsnummer = Bestillingsnummer("A-1-1"),
                    behandletAv = OkonomiPart.System(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                    besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                ),
            ),
        )
    }
})
