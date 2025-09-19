package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import arrow.core.raise.*
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TilsagnValidator {
    private const val TILSAGN_BESKRIVELSE_MAX_LENDE = 100

    data class Validated(
        val kostnadssted: NavEnhetNummer,
        val periode: Periode,
        val beregning: TilsagnBeregning,
    )

    fun validate(
        next: TilsagnRequest,
        previous: Tilsagn?,
        tiltakstypeNavn: String,
        arrangorSlettet: Boolean,
        gyldigTilsagnPeriode: Periode?,
        gjennomforingSluttDato: LocalDate?,
        avtalteSatser: List<AvtaltSats>,
    ): Either<List<FieldError>, Validated> = validation {
        validateNotNull(next.periodeStart) {
            FieldError.of("Periodestart må være satt", TilsagnRequest::periodeStart)
        }
        validateNotNull(next.periodeSlutt) {
            FieldError.of("Periodeslutt må være satt", TilsagnRequest::periodeSlutt)
        }
        validateNotNull(gyldigTilsagnPeriode) {
            FieldError.of("Tilsagn for tiltakstype $tiltakstypeNavn er ikke støttet enda", TilsagnRequest::periodeStart)
        }
        validate(!arrangorSlettet) {
            FieldError.of("Tilsagn kan ikke opprettes fordi arrangøren er slettet i Brreg", TilsagnRequest::id)
        }
        validate(previous == null || previous.status == TilsagnStatus.RETURNERT) {
            FieldError.of("Tilsagnet kan ikke endres", TilsagnRequest::id)
        }
        validate((next.kommentar?.length ?: 0) <= 500) {
            FieldError.of("Kommentar kan ikke inneholde mer enn 500 tegn", TilsagnRequest::kommentar)
        }
        validateNotNull(next.kostnadssted) {
            FieldError.of("Du må velge et kostnadssted", TilsagnRequest::kostnadssted)
        }

        validateAntallPlasser(next.beregning.type, next.beregning.antallPlasser).bind()
        validateAntallTimerOppfolgingPerDeltaker(next.beregning.type, next.beregning.antallTimerOppfolgingPerDeltaker).bind()
        requireValid(next.periodeStart != null && next.periodeSlutt != null && next.kostnadssted != null && gyldigTilsagnPeriode != null)

        validate(next.periodeStart.isBefore(next.periodeSlutt)) {
            FieldError.of("Periodestart må være før slutt", TilsagnRequest::periodeStart)
        }
        validate(!next.periodeStart.isBefore(gyldigTilsagnPeriode.start)) {
            FieldError.of("Minimum startdato for tilsagn til $tiltakstypeNavn er ${gyldigTilsagnPeriode.start.formaterDatoTilEuropeiskDatoformat()}", TilsagnRequest::periodeStart)
        }
        validate(!next.periodeSlutt.isAfter(gyldigTilsagnPeriode.getLastInclusiveDate())) {
            FieldError.of("Maksimum sluttdato for tilsagn til $tiltakstypeNavn er ${gyldigTilsagnPeriode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()}", TilsagnRequest::periodeSlutt)
        }
        validate(gjennomforingSluttDato == null || !next.periodeSlutt.isAfter(gjennomforingSluttDato)) {
            FieldError.of("Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden", TilsagnRequest::periodeSlutt)
        }
        validate(next.periodeStart.year == next.periodeSlutt.year) {
            FieldError.of("Tilsagnsperioden kan ikke vare utover årsskiftet", TilsagnRequest::periodeSlutt)
        }

        requireValid(next.periodeStart.isBefore(next.periodeSlutt))

        val periode = Periode.fromInclusiveDates(next.periodeStart, next.periodeSlutt)

        val sats = AvtalteSatser.findSats(avtalteSatser, periode.start)

        val beregning = validateBeregning(
            request = next.beregning,
            periode = periode,
            sats = sats,
            avtalteSatser,
        ).bind()

        Validated(
            beregning = beregning,
            periode = periode,
            kostnadssted = next.kostnadssted,
        )
    }

    fun validateAvtaltSats(
        beregningType: TilsagnBeregningType,
        avtalteSatser: List<AvtaltSats>,
        periode: Periode,
        sats: Int?,
    ): Either<List<FieldError>, Int> = validation {
        when (beregningType) {
            TilsagnBeregningType.FRI -> 0
            TilsagnBeregningType.PRIS_PER_MANEDSVERK,
            TilsagnBeregningType.PRIS_PER_UKESVERK,
            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING,
            -> {
                requireValid(sats != null) {
                    FieldError.of(
                        "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                        TilsagnRequest::periodeStart,
                    )
                }
                val satsPeriodeStart = AvtalteSatser.findSats(avtalteSatser, periode.start)
                validate(satsPeriodeStart != null) {
                    FieldError.of(
                        "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                        TilsagnRequest::periodeStart,
                    )
                }

                val satsPeriodeSlutt = AvtalteSatser.findSats(avtalteSatser, periode.getLastInclusiveDate())
                validate(satsPeriodeSlutt == null) {
                    FieldError.of(
                        "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                        TilsagnRequest::periodeSlutt,
                    )
                }
                validate(satsPeriodeStart != satsPeriodeSlutt) {
                    FieldError.of(
                        "Tilsagnsperioden kan ikke gå over flere registrerte sats-/prisperioder på avtalen",
                        TilsagnRequest::periodeSlutt,
                    )
                }

                validate(sats == satsPeriodeStart) {
                    FieldError.of(
                        "Sats må stemme med avtalt sats for perioden ($satsPeriodeStart)",
                        TilsagnRequest::periodeStart,
                    )
                }
                sats
            }
        }
    }

    fun validateBeregning(request: TilsagnBeregningRequest, periode: Periode, sats: Int?, avtalteSatser: List<AvtaltSats>): Either<List<FieldError>, TilsagnBeregning> = validation {
        val satsV = validateAvtaltSats(request.type, avtalteSatser, periode, sats).bind()
        val antallPlasser = validateAntallPlasser(request.type, request.antallPlasser).bind()
        val antallTimerOppfolgingPerDeltaker = validateAntallTimerOppfolgingPerDeltaker(request.type, request.antallTimerOppfolgingPerDeltaker).bind()

        when (request.type) {
            TilsagnBeregningType.FRI ->
                validateBeregningFriInput(request).bind()

            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED ->
                TilsagnBeregningFastSatsPerTiltaksplassPerManed.beregn(
                    TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                        periode = periode,
                        sats = satsV,
                        antallPlasser = antallPlasser,
                    ),
                )

            TilsagnBeregningType.PRIS_PER_MANEDSVERK ->
                TilsagnBeregningPrisPerManedsverk.beregn(
                    TilsagnBeregningPrisPerManedsverk.Input(
                        periode = periode,
                        sats = satsV,
                        antallPlasser = antallPlasser,
                        prisbetingelser = request.prisbetingelser,
                    ),
                )

            TilsagnBeregningType.PRIS_PER_UKESVERK ->
                TilsagnBeregningPrisPerUkesverk.beregn(
                    TilsagnBeregningPrisPerUkesverk.Input(
                        periode = periode,
                        sats = satsV,
                        antallPlasser = antallPlasser,
                        prisbetingelser = request.prisbetingelser,
                    ),
                )

            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING ->
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

    private fun validateAntallTimerOppfolgingPerDeltaker(type: TilsagnBeregningType, antallTimerOppfolgingPerDeltaker: Int?): Either<List<FieldError>, Int> = validation {
        when (type) {
            TilsagnBeregningType.FRI,
            TilsagnBeregningType.PRIS_PER_MANEDSVERK,
            TilsagnBeregningType.PRIS_PER_UKESVERK,
            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            -> 0
            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING -> {
                requireValid(antallTimerOppfolgingPerDeltaker != null && antallTimerOppfolgingPerDeltaker > 0) {
                    FieldError.of(
                        "Antall timer oppfølging per deltaker må være større enn 0",
                        TilsagnRequest::beregning,
                        TilsagnBeregningRequest::antallTimerOppfolgingPerDeltaker,
                    )
                }
                antallTimerOppfolgingPerDeltaker
            }
        }
    }

    private fun validateAntallPlasser(beregningType: TilsagnBeregningType, antallPlasser: Int?): Either<List<FieldError>, Int> = validation {
        when (beregningType) {
            TilsagnBeregningType.FRI -> 0
            TilsagnBeregningType.PRIS_PER_MANEDSVERK,
            TilsagnBeregningType.PRIS_PER_UKESVERK,
            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING,
            -> {
                requireValid(antallPlasser != null && antallPlasser > 0) {
                    FieldError.of(
                        "Antall plasser må være større enn 0",
                        TilsagnRequest::beregning,
                        TilsagnBeregningRequest::antallPlasser,
                    )
                }
                antallPlasser
            }
        }
    }

    fun validateBeregningFriInput(request: TilsagnBeregningRequest): Either<List<FieldError>, TilsagnBeregning> = validation {
        requireValid(!request.linjer.isNullOrEmpty()) {
            FieldError.of(
                "Du må legge til en linje",
                TilsagnRequest::beregning,
                TilsagnBeregningRequest::linjer,
            )
        }
        request.linjer.forEachIndexed { index, linje ->
            validate(linje.belop != null && linje.belop > 0) {
                FieldError.ofPointer(
                    pointer = "/beregning/linjer/$index/belop",
                    detail = "Beløp må være positivt",
                )
            }
            validate(!linje.beskrivelse.isNullOrBlank()) {
                FieldError.ofPointer(
                    pointer = "/beregning/linjer/$index/beskrivelse",
                    detail = "Beskrivelse mangler",
                )
            }
            validate(linje.beskrivelse.isNullOrBlank() || linje.beskrivelse.length <= TILSAGN_BESKRIVELSE_MAX_LENDE) {
                FieldError.ofPointer(
                    pointer = "/beregning/linjer/$index/beskrivelse",
                    detail = "Beskrivelsen kan ikke inneholde mer enn $TILSAGN_BESKRIVELSE_MAX_LENDE tegn",
                )
            }
            validate(linje.antall != null && linje.antall > 0) {
                FieldError.ofPointer(
                    pointer = "/beregning/linjer/$index/antall",
                    detail = "Antall må være positivt",
                )
            }
        }

        TilsagnBeregningFri.beregn(
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
        )
    }
}
