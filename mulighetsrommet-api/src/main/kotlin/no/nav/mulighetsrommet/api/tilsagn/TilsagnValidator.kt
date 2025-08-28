package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

object TilsagnValidator {
    fun validate(
        next: TilsagnRequest,
        previous: Tilsagn?,
        tiltakstypeNavn: String,
        arrangorSlettet: Boolean,
        minimumTilsagnPeriodeStart: LocalDate?,
        gjennomforingSluttDato: LocalDate?,
        avtalteSatser: List<AvtaltSats>,
    ): Either<NonEmptyList<FieldError>, Step3> {
        return validateStep1(
            next,
            previous,
            tiltakstypeNavn,
            arrangorSlettet,
            minimumTilsagnPeriodeStart,
        )
            .flatMap { step1 ->
                validateStep2(step1, gjennomforingSluttDato, tiltakstypeNavn)
            }
            .flatMap { step2 ->
                validateStep3(step2, next.beregning, avtalteSatser)
            }
    }

    data class Step1(
        val periodeStart: LocalDate,
        val periodeSlutt: LocalDate,
        val kostnadssted: NavEnhetNummer,
        val minimumTilsagnPeriodeStart: LocalDate,
    )

    fun validateStep1(
        next: TilsagnRequest,
        previous: Tilsagn?,
        tiltakstypeNavn: String,
        arrangorSlettet: Boolean,
        minimumTilsagnPeriodeStart: LocalDate?,
    ): Either<NonEmptyList<FieldError>, Step1> = either {
        zipOrAccumulate(
            {
                ensureNotNull(next.periodeStart) {
                    FieldError.of("Periodestart må være satt", TilsagnRequest::periodeStart)
                }
                next.periodeStart
            },
            {
                ensureNotNull(next.periodeSlutt) {
                    FieldError.of("Periodeslutt må være satt", TilsagnRequest::periodeSlutt)
                }
                next.periodeSlutt
            },
            {
                ensureNotNull(minimumTilsagnPeriodeStart) {
                    FieldError.of("Tilsagn for tiltakstype $tiltakstypeNavn er ikke støttet enda", TilsagnRequest::periodeStart)
                }
            },
            {
                ensure(!arrangorSlettet) {
                    FieldError.of(TilsagnRequest::id, "Tilsagn kan ikke opprettes fordi arrangøren er slettet i Brreg")
                }
            },
            {
                ensure(previous == null || previous.status == TilsagnStatus.RETURNERT) {
                    FieldError.of(TilsagnRequest::id, "Tilsagnet kan ikke endres.")
                }
            },
            {
                ensure((next.kommentar?.length ?: 0) <= 500) {
                    FieldError.of(TilsagnRequest::kommentar, "Kommentar kan ikke inneholde mer enn 500 tegn")
                }
            },
            {
                ensureNotNull(next.kostnadssted) {
                    FieldError.of(TilsagnRequest::kostnadssted, "Du må velge et kostnadssted")
                }
            },
        ) { start, slutt, minStart, _, _, _, kostnadssted ->
            Step1(periodeStart = start, periodeSlutt = slutt, kostnadssted = kostnadssted, minimumTilsagnPeriodeStart = minStart)
        }
    }

    data class Step2(
        val step1: Step1,
        val periode: Periode,
    )

    fun validateStep2(
        step1: Step1,
        gjennomforingSluttDato: LocalDate?,
        tiltakstypeNavn: String,
    ): Either<NonEmptyList<FieldError>, Step2> = either {
        zipOrAccumulate(
            {
                ensure(step1.periodeStart.isBefore(step1.periodeSlutt)) {
                    FieldError.of("Periodestart må være før slutt", TilsagnRequest::periodeStart)
                }
                Periode.fromInclusiveDates(step1.periodeStart, step1.periodeSlutt)
            },
            {
                ensure(!step1.periodeStart.isBefore(step1.minimumTilsagnPeriodeStart)) {
                    FieldError.of("Minimum startdato for tilsagn til $tiltakstypeNavn er ${step1.minimumTilsagnPeriodeStart.formaterDatoTilEuropeiskDatoformat()}", TilsagnRequest::periodeStart)
                }
            },
            {
                ensure(gjennomforingSluttDato == null || !step1.periodeSlutt.isAfter(gjennomforingSluttDato)) {
                    FieldError.of("Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden", TilsagnRequest::periodeSlutt)
                }
            },
            {
                ensure(step1.periodeStart.year == step1.periodeSlutt.year) {
                    FieldError.of(TilsagnRequest::periodeSlutt, "Tilsagnsperioden kan ikke vare utover årsskiftet")
                }
            },
        ) { periode, _, _, _ ->
            Step2(step1, periode)
        }
    }

    data class Step3(
        val step2: Step2,
        val beregning: TilsagnBeregning,
    )

    fun validateStep3(
        step2: Step2,
        request: TilsagnBeregningRequest?,
        avtalteSatser: List<AvtaltSats>,
    ): Either<NonEmptyList<FieldError>, Step3> = if (request == null) {
        FieldError.root("Beregning mangler").nel().left()
    } else {
        val sats = AvtalteSatser.findSats(avtalteSatser, step2.periode.start)
        validateBeregning(request, step2.periode, sats, avtalteSatser)
            .map { Step3(step2, it) }
    }

    fun validateAvtaltSats(
        avtalteSatser: List<AvtaltSats>,
        periode: Periode,
        sats: Int?,
    ): Either<NonEmptyList<FieldError>, Int> = either {
        val errors = buildList {
            if (sats == null) {
                return FieldError.of(
                    "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                    TilsagnRequest::periodeStart,
                ).nel().left()
            }
            val satsPeriodeStart = AvtalteSatser.findSats(avtalteSatser, periode.start)
            if (satsPeriodeStart == null) {
                add(
                    FieldError.of(
                        "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                        TilsagnRequest::periodeStart,
                    ),
                )
            }

            val satsPeriodeSlutt = AvtalteSatser.findSats(avtalteSatser, periode.getLastInclusiveDate())
            if (satsPeriodeSlutt == null) {
                add(
                    FieldError.of(
                        "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                        TilsagnRequest::periodeSlutt,
                    ),
                )
            } else if (satsPeriodeStart != satsPeriodeSlutt) {
                add(
                    FieldError.of(
                        "Tilsagnsperioden kan ikke gå over flere registrerte sats-/prisperioder på avtalen",
                        TilsagnRequest::periodeSlutt,
                    ),
                )
            }

            if (sats != satsPeriodeStart) {
                add(
                    FieldError.of(
                        "Sats må stemme med avtalt sats for perioden ($satsPeriodeStart)",
                        TilsagnRequest::periodeStart,
                    ),
                )
            }
        }

        return errors.toNonEmptyListOrNull()?.left() ?: sats!!.right()
    }

    fun validateBeregning(request: TilsagnBeregningRequest, periode: Periode, sats: Int?, avtalteSatser: List<AvtaltSats>): Either<NonEmptyList<FieldError>, TilsagnBeregning> = either {
        return when (request.type) {
            TilsagnBeregningType.FRI ->
                validateBeregningFriInput(request)

            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED ->
                either {
                    TilsagnBeregningFastSatsPerTiltaksplassPerManed.beregn(
                        TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                            periode = periode,
                            sats = validateAvtaltSats(avtalteSatser, periode, sats).bind(),
                            antallPlasser = validateAntallPlasser(request.antallPlasser).bind(),
                        ),
                    )
                }

            TilsagnBeregningType.PRIS_PER_MANEDSVERK ->
                either {
                    TilsagnBeregningPrisPerManedsverk.beregn(
                        TilsagnBeregningPrisPerManedsverk.Input(
                            periode = periode,
                            sats = validateAvtaltSats(avtalteSatser, periode, sats).bind(),
                            antallPlasser = validateAntallPlasser(request.antallPlasser).bind(),
                            prisbetingelser = request.prisbetingelser,
                        ),
                    )
                }

            TilsagnBeregningType.PRIS_PER_UKESVERK ->
                either {
                    TilsagnBeregningPrisPerUkesverk.beregn(
                        TilsagnBeregningPrisPerUkesverk.Input(
                            periode = periode,
                            sats = validateAvtaltSats(avtalteSatser, periode, sats).bind(),
                            antallPlasser = validateAntallPlasser(request.antallPlasser).bind(),
                            prisbetingelser = request.prisbetingelser,
                        ),
                    )
                }

            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING ->
                either {
                    TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(
                        TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                            periode = periode,
                            sats = validateAvtaltSats(avtalteSatser, periode, sats).bind(),
                            antallPlasser = validateAntallPlasser(request.antallPlasser).bind(),
                            prisbetingelser = request.prisbetingelser,
                            antallTimerOppfolgingPerDeltaker = validateAntallTimerOppfolgingPerDeltaker(request.antallTimerOppfolgingPerDeltaker).bind(),
                        ),
                    )
                }
        }
    }

    private fun validateAntallTimerOppfolgingPerDeltaker(antallTimerOppfolgingPerDeltaker: Int?): Either<NonEmptyList<FieldError>, Int> = if (antallTimerOppfolgingPerDeltaker == null || antallTimerOppfolgingPerDeltaker <= 0) {
        FieldError.of(
            "Antall timer oppfølging per deltaker kan ikke være 0",
            TilsagnRequest::beregning,
            TilsagnBeregningRequest::antallTimerOppfolgingPerDeltaker,
        ).nel().left()
    } else {
        antallTimerOppfolgingPerDeltaker.right()
    }

    private fun validateAntallPlasser(antallPlasser: Int?): Either<NonEmptyList<FieldError>, Int> = if (antallPlasser == null || antallPlasser <= 0) {
        FieldError.of(
            "Antall plasser kan ikke være 0",
            TilsagnRequest::beregning,
            TilsagnBeregningRequest::antallPlasser,
        ).nel().left()
    } else {
        antallPlasser.right()
    }

    private fun validateBeregningFriInput(request: TilsagnBeregningRequest): Either<NonEmptyList<FieldError>, TilsagnBeregning> = either {
        if (request.linjer.isNullOrEmpty()) {
            return FieldError.ofPointer(
                pointer = "beregning/linjer",
                detail = "Du må legge til en linje",
            ).nel().left()
        }
        val errors = buildList {
            request.linjer.forEachIndexed { index, linje ->
                if (linje.belop == null || linje.belop <= 0) {
                    add(
                        FieldError.ofPointer(
                            pointer = "beregning/linjer/$index/belop",
                            detail = "Beløp må være positivt",
                        ),
                    )
                }
                if (linje.beskrivelse.isNullOrBlank()) {
                    add(
                        FieldError.ofPointer(
                            pointer = "beregning/linjer/$index/beskrivelse",
                            detail = "Beskrivelse mangler",
                        ),
                    )
                }
                if (linje.antall == null || linje.antall <= 0) {
                    add(
                        FieldError.ofPointer(
                            pointer = "beregning/linjer/$index/antall",
                            detail = "Antall må være positivt",
                        ),
                    )
                }
            }
        }

        return errors.toNonEmptyListOrNull()?.left()
            ?: TilsagnBeregningFri.beregn(
                TilsagnBeregningFri.Input(
                    linjer = request.linjer.map {
                        TilsagnBeregningFri.InputLinje(
                            id = it.id,
                            beskrivelse = it.beskrivelse!!,
                            belop = it.belop!!,
                            antall = it.antall!!,
                        )
                    },
                    prisbetingelser = request.prisbetingelser,
                ),
            ).right()
    }
}
