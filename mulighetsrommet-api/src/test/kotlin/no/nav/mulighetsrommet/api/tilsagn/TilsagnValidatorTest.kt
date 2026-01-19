package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnInputLinjeRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate
import java.util.UUID

class TilsagnValidatorTest : FunSpec({
    context("validate Tilsagn") {
        test("Arrangør slettet") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1,
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = true,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Tilsagn kan ikke opprettes fordi arrangøren er slettet i Brreg", TilsagnRequest::id),
            )
        }

        test("mangler sats i periode") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1.copy(
                    beregning = TilsagnBeregningRequest(
                        type = TilsagnBeregningType.PRIS_PER_MANEDSVERK,
                        antallPlasser = 3,
                        valuta = Valuta.NOK,
                    ),
                ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of(
                    "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                    TilsagnRequest::periodeStart,
                ),
            )
        }

        test("ingen feil på gyldig AFT pris per månedsverk") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1.copy(
                    beregning = TilsagnBeregningRequest(
                        type = TilsagnBeregningType.PRIS_PER_MANEDSVERK,
                        antallPlasser = 3,
                        valuta = Valuta.NOK,
                    ),
                ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = listOf(AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.withValuta(Valuta.NOK))),
            ).shouldBeRight()
        }

        test("null antall plasser samtidig som null periodeStart") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1.copy(
                    periodeStart = null,
                    beregning = TilsagnBeregningRequest(
                        type = TilsagnBeregningType.PRIS_PER_MANEDSVERK,
                        valuta = Valuta.NOK,
                    ),
                ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Periodestart må være satt", TilsagnRequest::periodeStart),
                FieldError.ofPointer(
                    pointer = "/beregning/antallPlasser",
                    detail = "Antall plasser må være større enn 0",
                ),
            )
        }

        test("should validate gyldig periode") {
            val gyldigStart = LocalDate.of(2025, 1, 8)
            val gyldigSlutt = LocalDate.of(2025, 1, 9)
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1,
                previous = null,
                gyldigTilsagnPeriode = Periode.fromInclusiveDates(gyldigStart, gyldigSlutt),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of(
                    "Minimum startdato for tilsagn til AFT er ${gyldigStart.formaterDatoTilEuropeiskDatoformat()}",
                    TilsagnRequest::periodeStart,
                ),
                FieldError.of(
                    "Maksimum sluttdato for tilsagn til AFT er ${gyldigSlutt.formaterDatoTilEuropeiskDatoformat()}",
                    TilsagnRequest::periodeSlutt,
                ),
            )
        }

        test("feil i beregning dukker opp") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1
                    .copy(
                        beregning = TilsagnBeregningRequest(type = TilsagnBeregningType.FRI, valuta = Valuta.NOK),
                    ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Du må legge til en linje", TilsagnRequest::beregning, TilsagnBeregningRequest::linjer),
            )
        }

        test("antall plasser 0 og kostnadssted samles") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1
                    .copy(
                        kostnadssted = null,
                        beregning = TilsagnBeregningRequest(
                            type = TilsagnBeregningType.PRIS_PER_MANEDSVERK,
                        ),
                    ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Du må velge et kostnadssted", TilsagnRequest::kostnadssted),
                FieldError.of(
                    "Antall plasser må være større enn 0",
                    TilsagnRequest::beregning,
                    TilsagnBeregningRequest::antallPlasser,
                ),
            )
        }

        test("feil ved utenfor gyldig periode") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1.copy(
                    periodeStart = LocalDate.of(2026, 1, 1),
                    periodeSlutt = LocalDate.of(2026, 4, 1),
                    beregning = TilsagnBeregningRequest(
                        type = TilsagnBeregningType.PRIS_PER_MANEDSVERK,
                        antallPlasser = 3,
                        valuta = Valuta.NOK,
                    ),
                ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = listOf(AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.withValuta(Valuta.NOK))),
            ) shouldBeLeft listOf(
                FieldError.of("Maksimum sluttdato for tilsagn til AFT er 31.12.2025", TilsagnRequest::periodeSlutt),
            )
        }

        test("minimum dato for tilsagn må være satt for at tilsagn skal opprettes") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1,
                previous = null,
                gyldigTilsagnPeriode = null,
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = listOf(AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.withValuta(Valuta.NOK))),
            ) shouldBeLeft listOf(
                FieldError(
                    pointer = "/periodeStart",
                    detail = "Tilsagn for tiltakstype AFT er ikke støttet enda",
                ),
            )
        }

        test("tilsagnet kan ikke slutte etter gjennomføringen") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1.copy(periodeSlutt = LocalDate.of(2025, 11, 1)),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = LocalDate.of(2025, 10, 1),
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = listOf(AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.withValuta(Valuta.NOK))),
            ) shouldBeLeft listOf(
                FieldError(
                    pointer = "/periodeSlutt",
                    detail = "Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden",
                ),
            )
        }

        test("tilsagnet må ha en valuta") {
            TilsagnValidator.validate(
                next =
                TilsagnFixtures.TilsagnRequest1.copy(
                    beregning = TilsagnFixtures.TilsagnRequest1.beregning.copy(valuta = null),
                ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = listOf(AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.withValuta(Valuta.NOK))),
            ).shouldBeLeft()
        }

        context("TilsagnBeregningFri.Input") {
            test("should return field error if linjer is empty") {
                val input = TilsagnBeregningRequest(
                    type = TilsagnBeregningType.FRI,
                    linjer = emptyList(),
                    prisbetingelser = null,
                )
                TilsagnValidator.validateBeregningFriInput(input).shouldBeLeft()
            }

            test("should return list of field error for invalid input") {
                val input = TilsagnBeregningRequest(
                    type = TilsagnBeregningType.FRI,
                    linjer = listOf(
                        TilsagnInputLinjeRequest(
                            pris = 0.withValuta(Valuta.NOK),
                            id = UUID.randomUUID(),
                            beskrivelse = "",
                            antall = 0,
                        ),
                    ),
                    prisbetingelser = null,
                )
                val leftFieldErrors = listOf(
                    FieldError(pointer = "/beregning/linjer/0/belop", detail = "Beløp må være positivt"),
                    FieldError(pointer = "/beregning/linjer/0/beskrivelse", detail = "Beskrivelse mangler"),
                    FieldError(pointer = "/beregning/linjer/0/antall", detail = "Antall må være positivt"),
                )

                TilsagnValidator.validateBeregningFriInput(input) shouldBeLeft leftFieldErrors
            }
        }
    }
})
