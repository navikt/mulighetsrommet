package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.avtale.model.ForhandsgodkjenteSatser
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate

object TilsagnValidator {
    fun validate(
        next: TilsagnRequest,
        previous: Tilsagn?,
        tiltakstypeNavn: String,
        gjennomforingSluttDato: LocalDate?,
        arrangorSlettet: Boolean,
        minimumTilsagnPeriodeStart: LocalDate?,
    ): Either<List<FieldError>, TilsagnRequest> = either {
        val errors = buildList {
            if (minimumTilsagnPeriodeStart == null) {
                add(
                    FieldError
                        .of(
                            "Tilsagn for tiltakstype $tiltakstypeNavn er ikke støttet enda",
                            TilsagnRequest::periodeStart,
                        ),
                )
            } else if (next.periodeStart < minimumTilsagnPeriodeStart) {
                add(
                    FieldError
                        .of(
                            "Minimum startdato for tilsagn til $tiltakstypeNavn er ${minimumTilsagnPeriodeStart.formaterDatoTilEuropeiskDatoformat()}",
                            TilsagnRequest::periodeStart,
                        ),
                )
            } else if (gjennomforingSluttDato !== null && next.periodeSlutt > gjennomforingSluttDato) {
                add(
                    FieldError.of(
                        "Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden",
                        TilsagnRequest::periodeSlutt,
                    ),
                )
            }
            if (arrangorSlettet) {
                add(
                    FieldError
                        .of(
                            TilsagnRequest::id,
                            "Tilsagn kan ikke opprettes fordi arrangøren er slettet i Brreg",
                        ),
                )
            }
            if (previous != null && previous.status != TilsagnStatus.RETURNERT) {
                return FieldError
                    .of(Tilsagn::id, "Tilsagnet kan ikke endres.")
                    .nel()
                    .left()
            }
            if (next.periodeStart.year != next.periodeSlutt.year) {
                add(
                    FieldError.of(
                        TilsagnRequest::periodeSlutt,
                        "Tilsagnsperioden kan ikke vare utover årsskiftet",
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: next.right()
    }

    fun validateForhandsgodkjentSats(
        tiltakskode: Tiltakskode,
        input: TilsagnBeregningAvtaltPrisPerManedsverk.Input,
    ): Either<List<FieldError>, TilsagnBeregningAvtaltPrisPerManedsverk.Input> = either {
        val errors = buildList {
            val satsPeriodeStart = ForhandsgodkjenteSatser.findSats(tiltakskode, input.periode.start)
            if (satsPeriodeStart == null) {
                add(
                    FieldError.of(
                        "Tilsagn kan ikke registreres for valgt periode enda",
                        TilsagnRequest::periodeStart,
                    ),
                )
            }

            val satsPeriodeSlutt = ForhandsgodkjenteSatser.findSats(tiltakskode, input.periode.getLastInclusiveDate())
            if (satsPeriodeSlutt == null) {
                add(
                    FieldError.of(
                        "Tilsagn kan ikke registreres for valgt periode enda",
                        TilsagnRequest::periodeSlutt,
                    ),
                )
            }

            if (satsPeriodeStart != satsPeriodeSlutt) {
                add(
                    FieldError.of(
                        "Periode går over flere satser",
                        TilsagnRequest::periodeSlutt,
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }

    fun validateBeregningInput(input: TilsagnBeregningInput): Either<List<FieldError>, TilsagnBeregningInput> = either {
        return when (input) {
            is TilsagnBeregningFri.Input -> validateBeregningFriInput(input)
            is TilsagnBeregningAvtaltPrisPerManedsverk.Input -> validateBeregningAvtaltPrisPerManedsverkInput(input)
        }
    }

    private fun validateBeregningAvtaltPrisPerManedsverkInput(input: TilsagnBeregningAvtaltPrisPerManedsverk.Input): Either<List<FieldError>, TilsagnBeregningInput> = either {
        val errors = buildList {
            if (input.periode.start.year != input.periode.getLastInclusiveDate().year) {
                add(
                    FieldError.of(
                        "Tilsagnsperioden kan ikke vare utover årsskiftet",
                        TilsagnBeregningAvtaltPrisPerManedsverk.Input::periode,
                        Periode::slutt,
                    ),
                )
            }
            if (input.antallPlasser <= 0) {
                add(
                    FieldError.of(
                        "Antall plasser kan ikke være 0",
                        TilsagnBeregningAvtaltPrisPerManedsverk.Input::antallPlasser,
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }

    private fun validateBeregningFriInput(input: TilsagnBeregningFri.Input): Either<List<FieldError>, TilsagnBeregningInput> = either {
        if (input.linjer.isEmpty()) {
            return listOf(
                FieldError.ofPointer(
                    pointer = "beregning/linjer",
                    detail = "Du må legge til en linje",
                ),
            ).left()
        }
        val errors = buildList {
            input.linjer.forEachIndexed { index, linje ->
                if (linje.belop <= 0) {
                    add(
                        FieldError.ofPointer(
                            pointer = "beregning/linjer/$index/belop",
                            detail = "Beløp må være positivt",
                        ),
                    )
                }
                if (linje.beskrivelse.isBlank()) {
                    add(
                        FieldError.ofPointer(
                            pointer = "beregning/linjer/$index/beskrivelse",
                            detail = "Beskrivelse mangler",
                        ),
                    )
                }
                if (linje.antall <= 0) {
                    add(
                        FieldError.ofPointer(
                            pointer = "beregning/linjer/$index/antall",
                            detail = "Antall må være positivt",
                        ),
                    )
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }
}
