package no.nav.tiltak.okonomi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.test.assertContract
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
                    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
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
                    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.EKSPERTBISTAND),
                    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.EKSPERTBISTAND),
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
                    besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
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
                    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
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
                    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
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
                    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                    besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                ),
            ),
        )
    }

    context("bakoverkompatibilitet med gammelt meldingsformat") {
        fun assertLegacyDecodesTo(legacyJson: String, expected: OkonomiBestillingMelding) {
            decodeOkonomiBestillingMelding(Json.parseToJsonElement(legacyJson)) shouldBe expected
        }

        test("BESTILLING med norsk arrangør, gammel diskriminator og gammelt part-felt") {
            assertLegacyDecodesTo(
                """
                {
                  "type": "BESTILLING",
                  "payload": {
                    "bestillingsnummer": "A-1-1",
                    "fagsystem": "TILTAKSADMINISTRASJON",
                    "tilskuddstype": "TILTAK_DRIFTSTILSKUDD",
                    "tiltakskode": "ARBEIDSFORBEREDENDE_TRENING",
                    "arrangor": {
                      "type": "no.nav.tiltak.okonomi.OpprettBestilling.Arrangor.Norsk",
                      "organisasjonsnummer": "123456789"
                    },
                    "kostnadssted": "0400",
                    "avtalenummer": "avtale-1",
                    "belop": 1000,
                    "periode": {
                      "start": "2025-01-01",
                      "slutt": "2025-02-01"
                    },
                    "behandletAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.System",
                      "part": "TILTAKSADMINISTRASJON",
                      "kilde": "TILTAKSADMINISTRASJON"
                    },
                    "behandletTidspunkt": "2025-01-01T00:00:00Z",
                    "besluttetAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.NavAnsatt",
                      "part": "Z123456",
                      "navIdent": "Z123456"
                    },
                    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
                    "valuta": "NOK"
                  }
                }
                """.trimIndent(),
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
                        behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                        behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                        besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                        besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                        valuta = Valuta.NOK,
                    ),
                ),
            )
        }

        test("BESTILLING med utenlandsk arrangør og gammel diskriminator") {
            assertLegacyDecodesTo(
                """
                {
                  "type": "BESTILLING",
                  "payload": {
                    "bestillingsnummer": "E-1-1",
                    "fagsystem": "EKSPERTBISTAND",
                    "tilskuddstype": "TILTAK_DRIFTSTILSKUDD",
                    "tiltakskode": "ARBEIDSFORBEREDENDE_TRENING",
                    "arrangor": {
                      "type": "no.nav.tiltak.okonomi.OpprettBestilling.Arrangor.Utenlandsk",
                      "organisasjonsnummer": "123456789",
                      "navn": "Utenlandsk Arrangør AS",
                      "gateNavn": "Main Street 1",
                      "by": "Stockholm",
                      "postNummer": "12345",
                      "landKode": "SE"
                    },
                    "kostnadssted": "0400",
                    "avtalenummer": null,
                    "belop": 2000,
                    "periode": {
                      "start": "2025-01-01",
                      "slutt": "2025-02-01"
                    },
                    "behandletAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.System",
                      "part": "EKSPERTBISTAND",
                      "kilde": "EKSPERTBISTAND"
                    },
                    "behandletTidspunkt": "2025-01-01T00:00:00Z",
                    "besluttetAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.System",
                      "part": "EKSPERTBISTAND",
                      "kilde": "EKSPERTBISTAND"
                    },
                    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
                    "valuta": "NOK"
                  }
                }
                """.trimIndent(),
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
                        behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.EKSPERTBISTAND),
                        behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                        besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.EKSPERTBISTAND),
                        besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                        valuta = Valuta.NOK,
                    ),
                ),
            )
        }

        test("ANNULLERING med gammel diskriminator og gammelt part-felt") {
            assertLegacyDecodesTo(
                """
                {
                  "type": "ANNULLERING",
                  "payload": {
                    "bestillingsnummer": "A-1-1",
                    "behandletAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.NavAnsatt",
                      "part": "Z123456",
                      "navIdent": "Z123456"
                    },
                    "behandletTidspunkt": "2025-01-03T00:00:00Z",
                    "besluttetAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.System",
                      "part": "TILTAKSADMINISTRASJON",
                      "kilde": "TILTAKSADMINISTRASJON"
                    },
                    "besluttetTidspunkt": "2025-01-04T00:00:00Z"
                  }
                }
                """.trimIndent(),
                OkonomiBestillingMelding.Annullering(
                    payload = AnnullerBestilling(
                        bestillingsnummer = Bestillingsnummer("A-1-1"),
                        behandletAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                        behandletTidspunkt = Instant.parse("2025-01-03T00:00:00Z"),
                        besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                        besluttetTidspunkt = Instant.parse("2025-01-04T00:00:00Z"),
                    ),
                ),
            )
        }

        test("FAKTURA med BBan-betalingsinformasjon og gammel diskriminator") {
            assertLegacyDecodesTo(
                """
                {
                  "type": "FAKTURA",
                  "payload": {
                    "fakturanummer": "A-1-1-1",
                    "bestillingsnummer": "A-1-1",
                    "betalingsinformasjon": {
                      "type": "no.nav.tiltak.okonomi.OpprettFaktura.Betalingsinformasjon.BBan",
                      "kontonummer": "12345678901",
                      "kid": "0004614992"
                    },
                    "belop": 1000,
                    "periode": {
                      "start": "2025-01-01",
                      "slutt": "2025-02-01"
                    },
                    "behandletAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.System",
                      "part": "TILTAKSADMINISTRASJON",
                      "kilde": "TILTAKSADMINISTRASJON"
                    },
                    "behandletTidspunkt": "2025-01-01T00:00:00Z",
                    "besluttetAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.NavAnsatt",
                      "part": "Z123456",
                      "navIdent": "Z123456"
                    },
                    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
                    "gjorOppBestilling": false,
                    "beskrivelse": "En beskrivelse",
                    "valuta": "NOK"
                  }
                }
                """.trimIndent(),
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
                        behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
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

        test("FAKTURA med IBan-betalingsinformasjon og gammel diskriminator") {
            assertLegacyDecodesTo(
                """
                {
                  "type": "FAKTURA",
                  "payload": {
                    "fakturanummer": "A-1-1-1",
                    "bestillingsnummer": "A-1-1",
                    "betalingsinformasjon": {
                      "type": "no.nav.tiltak.okonomi.OpprettFaktura.Betalingsinformasjon.IBan",
                      "bic": "DABANO22",
                      "iban": "NO9386011117947",
                      "bankNavn": "DNB",
                      "bankLandKode": "NO"
                    },
                    "belop": 1000,
                    "periode": {
                      "start": "2025-01-01",
                      "slutt": "2025-02-01"
                    },
                    "behandletAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.System",
                      "part": "TILTAKSADMINISTRASJON",
                      "kilde": "TILTAKSADMINISTRASJON"
                    },
                    "behandletTidspunkt": "2025-01-01T00:00:00Z",
                    "besluttetAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.NavAnsatt",
                      "part": "Z123456",
                      "navIdent": "Z123456"
                    },
                    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
                    "gjorOppBestilling": true,
                    "beskrivelse": null,
                    "valuta": "NOK"
                  }
                }
                """.trimIndent(),
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
                        behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
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

        test("GJOR_OPP_BESTILLING med gammel diskriminator og gammelt part-felt") {
            assertLegacyDecodesTo(
                """
                {
                  "type": "GJOR_OPP_BESTILLING",
                  "payload": {
                    "bestillingsnummer": "A-1-1",
                    "behandletAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.System",
                      "part": "TILTAKSADMINISTRASJON",
                      "kilde": "TILTAKSADMINISTRASJON"
                    },
                    "behandletTidspunkt": "2025-01-01T00:00:00Z",
                    "besluttetAv": {
                      "type": "no.nav.tiltak.okonomi.OkonomiPart.NavAnsatt",
                      "part": "Z123456",
                      "navIdent": "Z123456"
                    },
                    "besluttetTidspunkt": "2025-01-02T00:00:00Z"
                  }
                }
                """.trimIndent(),
                OkonomiBestillingMelding.GjorOppBestilling(
                    payload = GjorOppBestilling(
                        bestillingsnummer = Bestillingsnummer("A-1-1"),
                        behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                        behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                        besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                        besluttetTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                    ),
                ),
            )
        }
    }
})
