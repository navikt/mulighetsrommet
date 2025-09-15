package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import arrow.core.raise.*
import arrow.core.raise.ensure
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

object TilsagnValidator {
    private const val TILSAGN_BESKRIVELSE_MAX_LENDE = 100
    fun validate(
        next: TilsagnRequest,
        previous: Tilsagn?,
        tiltakstypeNavn: String,
        arrangorSlettet: Boolean,
        gyldigTilsagnPeriode: Periode?,
        gjennomforingSluttDato: LocalDate?,
        avtalteSatser: List<AvtaltSats>,
    ): Either<NonEmptyList<FieldError>, Step3> {
        return validateStep1(
            next,
            previous,
            tiltakstypeNavn,
            arrangorSlettet,
            gyldigTilsagnPeriode,
        )
            .flatMap { step1 ->
                validateStep2(step1, gjennomforingSluttDato, tiltakstypeNavn)
            }
            .flatMap { step2 ->
                validateStep3(
                    step2,
                    next.beregning,
                    avtalteSatser,
                )
            }
    }

    data class Step1(
        val periodeStart: LocalDate,
        val periodeSlutt: LocalDate,
        val kostnadssted: NavEnhetNummer,
        val gyldigTilsagnPeriode: Periode,
    )

    fun validateStep1(
        next: TilsagnRequest,
        previous: Tilsagn?,
        tiltakstypeNavn: String,
        arrangorSlettet: Boolean,
        gyldigTilsagnPeriode: Periode?,
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
                ensureNotNull(gyldigTilsagnPeriode) {
                    FieldError.of("Tilsagn for tiltakstype $tiltakstypeNavn er ikke støttet enda", TilsagnRequest::periodeStart)
                }
            },
            {
                ensure(!arrangorSlettet) {
                    FieldError.of("Tilsagn kan ikke opprettes fordi arrangøren er slettet i Brreg", TilsagnRequest::id)
                }
            },
            {
                ensure(previous == null || previous.status == TilsagnStatus.RETURNERT) {
                    FieldError.of("Tilsagnet kan ikke endres.", TilsagnRequest::id)
                }
            },
            {
                ensure((next.kommentar?.length ?: 0) <= 500) {
                    FieldError.of("Kommentar kan ikke inneholde mer enn 500 tegn", TilsagnRequest::kommentar)
                }
            },
            {
                ensureNotNull(next.kostnadssted) {
                    FieldError.of("Du må velge et kostnadssted", TilsagnRequest::kostnadssted)
                }
            },
            {
                validateAntallPlasser(next.beregning.type, next.beregning.antallPlasser).bind()
            },
            {
                validateAntallTimerOppfolgingPerDeltaker(next.beregning.type, next.beregning.antallTimerOppfolgingPerDeltaker).bind()
            },
        ) { start, slutt, gyldigPeriode, _, _, _, kostnadssted, _, _ ->
            Step1(
                periodeStart = start,
                periodeSlutt = slutt,
                kostnadssted = kostnadssted,
                gyldigTilsagnPeriode = gyldigPeriode,
            )
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
                ensure(!step1.periodeStart.isBefore(step1.gyldigTilsagnPeriode.start)) {
                    FieldError.of("Minimum startdato for tilsagn til $tiltakstypeNavn er ${step1.gyldigTilsagnPeriode.start.formaterDatoTilEuropeiskDatoformat()}", TilsagnRequest::periodeStart)
                }
            },
            {
                ensure(!step1.periodeSlutt.isAfter(step1.gyldigTilsagnPeriode.getLastInclusiveDate())) {
                    FieldError.of("Maksimum sluttdato for tilsagn til $tiltakstypeNavn er ${step1.gyldigTilsagnPeriode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()}", TilsagnRequest::periodeSlutt)
                }
            },
            {
                ensure(gjennomforingSluttDato == null || !step1.periodeSlutt.isAfter(gjennomforingSluttDato)) {
                    FieldError.of("Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden", TilsagnRequest::periodeSlutt)
                }
            },
            {
                ensure(step1.periodeStart.year == step1.periodeSlutt.year) {
                    FieldError.of("Tilsagnsperioden kan ikke vare utover årsskiftet", TilsagnRequest::periodeSlutt)
                }
            },
        ) { periode, _, _, _, _ ->
            Step2(step1, periode)
        }
    }

    data class Step3(
        val step2: Step2,
        val beregning: TilsagnBeregning,
    )

    fun validateStep3(
        step2: Step2,
        request: TilsagnBeregningRequest,
        avtalteSatser: List<AvtaltSats>,
    ): Either<NonEmptyList<FieldError>, Step3> {
        val sats = AvtalteSatser.findSats(avtalteSatser, step2.periode.start)
        return validateBeregning(
            request = request,
            periode = step2.periode,
            sats = sats,
            avtalteSatser,
        )
            .map { Step3(step2, it) }
    }

    fun validateAvtaltSats(
        beregningType: TilsagnBeregningType,
        avtalteSatser: List<AvtaltSats>,
        periode: Periode,
        sats: Int?,
    ): Either<NonEmptyList<FieldError>, Int> = either {
        when (beregningType) {
            TilsagnBeregningType.FRI -> return 0.right()
            TilsagnBeregningType.PRIS_PER_MANEDSVERK,
            TilsagnBeregningType.PRIS_PER_UKESVERK,
            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING,
            -> Unit
        }
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
        val satsV = validateAvtaltSats(request.type, avtalteSatser, periode, sats).bind()
        val antallPlasser = validateAntallPlasser(request.type, request.antallPlasser)
            .mapLeft { it.nel() }.bind()
        val antallTimerOppfolgingPerDeltaker = validateAntallTimerOppfolgingPerDeltaker(request.type, request.antallTimerOppfolgingPerDeltaker)
            .mapLeft { it.nel() }.bind()

        return when (request.type) {
            TilsagnBeregningType.FRI ->
                validateBeregningFriInput(request)

            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED ->
                either {
                    TilsagnBeregningFastSatsPerTiltaksplassPerManed.beregn(
                        TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                            periode = periode,
                            sats = satsV,
                            antallPlasser = antallPlasser,
                        ),
                    )
                }

            TilsagnBeregningType.PRIS_PER_MANEDSVERK ->
                either {
                    TilsagnBeregningPrisPerManedsverk.beregn(
                        TilsagnBeregningPrisPerManedsverk.Input(
                            periode = periode,
                            sats = satsV,
                            antallPlasser = antallPlasser,
                            prisbetingelser = request.prisbetingelser,
                        ),
                    )
                }

            TilsagnBeregningType.PRIS_PER_UKESVERK ->
                either {
                    TilsagnBeregningPrisPerUkesverk.beregn(
                        TilsagnBeregningPrisPerUkesverk.Input(
                            periode = periode,
                            sats = satsV,
                            antallPlasser = antallPlasser,
                            prisbetingelser = request.prisbetingelser,
                        ),
                    )
                }

            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING ->
                either {
                    TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(
                        TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                            periode = periode,
                            sats = satsV,
                            antallPlasser = antallPlasser,
                            prisbetingelser = request.prisbetingelser,
                            antallTimerOppfolgingPerDeltaker = antallTimerOppfolgingPerDeltaker,
                        ),
                    )
                }
        }
    }

    private fun validateAntallTimerOppfolgingPerDeltaker(type: TilsagnBeregningType, antallTimerOppfolgingPerDeltaker: Int?): Either<FieldError, Int> {
        return when (type) {
            TilsagnBeregningType.FRI,
            TilsagnBeregningType.PRIS_PER_MANEDSVERK,
            TilsagnBeregningType.PRIS_PER_UKESVERK,
            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            -> 0.right()
            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING -> {
                if (antallTimerOppfolgingPerDeltaker == null || antallTimerOppfolgingPerDeltaker <= 0) {
                    FieldError.of(
                        "Antall timer oppfølging per deltaker må være større enn 0",
                        TilsagnRequest::beregning,
                        TilsagnBeregningRequest::antallTimerOppfolgingPerDeltaker,
                    ).left()
                } else {
                    antallTimerOppfolgingPerDeltaker.right()
                }
            }
        }
    }

    private fun validateAntallPlasser(beregningType: TilsagnBeregningType, antallPlasser: Int?): Either<FieldError, Int> {
        return when (beregningType) {
            TilsagnBeregningType.FRI -> 0.right()
            TilsagnBeregningType.PRIS_PER_MANEDSVERK,
            TilsagnBeregningType.PRIS_PER_UKESVERK,
            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING,
            -> {
                if (antallPlasser == null || antallPlasser <= 0) {
                    FieldError.of(
                        "Antall plasser må være større enn 0",
                        TilsagnRequest::beregning,
                        TilsagnBeregningRequest::antallPlasser,
                    ).left()
                } else {
                    antallPlasser.right()
                }
            }
        }
    }

    fun validateBeregningFriInput(request: TilsagnBeregningRequest): Either<NonEmptyList<FieldError>, TilsagnBeregning> = either {
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
                if (linje.beskrivelse?.let { it.length > TILSAGN_BESKRIVELSE_MAX_LENDE } == true) {
                    add(
                        FieldError.ofPointer(
                            pointer = "beregning/linjer/$index/beskrivelse",
                            detail = "Beskrivelsen kan ikke inneholde mer enn ${TILSAGN_BESKRIVELSE_MAX_LENDE} tegn",
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
