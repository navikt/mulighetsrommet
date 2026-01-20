package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.findAvtaltSats
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.validation.FieldValidator
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
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
        prismodellValuta: Valuta,
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
        validate((next.beskrivelse?.length ?: 0) <= 250) {
            FieldError.of("Beskrivelse kan ikke inneholde mer enn 250 tegn", TilsagnRequest::beskrivelse)
        }
        validateNotNull(next.kostnadssted) {
            FieldError.of("Du må velge et kostnadssted", TilsagnRequest::kostnadssted)
        }

        validateAntallPlasser(next.beregning.type, next.beregning.antallPlasser)
        validateAntallTimerOppfolgingPerDeltaker(next.beregning.type, next.beregning.antallTimerOppfolgingPerDeltaker)
        requireValid(next.periodeStart != null && next.periodeSlutt != null && next.kostnadssted != null && gyldigTilsagnPeriode != null)

        validate(next.periodeStart.isBefore(next.periodeSlutt)) {
            FieldError.of("Periodestart må være før slutt", TilsagnRequest::periodeStart)
        }
        validate(!next.periodeStart.isBefore(gyldigTilsagnPeriode.start)) {
            val dato = gyldigTilsagnPeriode.start.formaterDatoTilEuropeiskDatoformat()
            FieldError.of("Minimum startdato for tilsagn til $tiltakstypeNavn er $dato", TilsagnRequest::periodeStart)
        }
        validate(!next.periodeSlutt.isAfter(gyldigTilsagnPeriode.getLastInclusiveDate())) {
            val dato = gyldigTilsagnPeriode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
            FieldError.of("Maksimum sluttdato for tilsagn til $tiltakstypeNavn er $dato", TilsagnRequest::periodeSlutt)
        }
        validate(gjennomforingSluttDato == null || !next.periodeSlutt.isAfter(gjennomforingSluttDato)) {
            FieldError.of(
                "Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden",
                TilsagnRequest::periodeSlutt,
            )
        }
        validate(next.periodeStart.year == next.periodeSlutt.year) {
            FieldError.of("Tilsagnsperioden kan ikke vare utover årsskiftet", TilsagnRequest::periodeSlutt)
        }

        requireValid(next.periodeStart.isBefore(next.periodeSlutt))

        val periode = Periode.fromInclusiveDates(next.periodeStart, next.periodeSlutt)

        val beregning = validateBeregning(
            request = next.beregning,
            prismodellValuta,
            periode = periode,
            avtalteSatser = avtalteSatser,
        )

        validate(beregning.output.pris.belop > 0) {
            FieldError.root("Beløp må være større enn 0")
        }

        Validated(
            beregning = beregning,
            periode = periode,
            kostnadssted = next.kostnadssted,
        )
    }

    fun FieldValidator.validateAvtaltSats(
        beregningType: TilsagnBeregningType,
        valuta: Valuta,
        avtalteSatser: List<AvtaltSats>,
        periode: Periode,
    ): ValutaBelop = when (beregningType) {
        TilsagnBeregningType.FRI -> ValutaBelop(0, valuta)

        TilsagnBeregningType.PRIS_PER_MANEDSVERK,
        TilsagnBeregningType.PRIS_PER_UKESVERK,
        TilsagnBeregningType.PRIS_PER_HELE_UKESVERK,
        TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
        TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING,
        -> {
            val satsPeriodeStart = avtalteSatser.findAvtaltSats(periode.start)
            requireValid(satsPeriodeStart != null) {
                FieldError.of(
                    "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                    TilsagnRequest::periodeStart,
                )
            }

            val satsPeriodeSlutt = avtalteSatser.findAvtaltSats(periode.getLastInclusiveDate())
            validate(satsPeriodeSlutt != null) {
                FieldError.of(
                    "Tilsagn kan ikke registreres for perioden fordi det mangler registrert sats/avtalt pris",
                    TilsagnRequest::periodeSlutt,
                )
            }
            validate(satsPeriodeStart.sats == satsPeriodeSlutt?.sats) {
                FieldError.of(
                    "Tilsagnsperioden kan ikke gå over flere registrerte sats-/prisperioder på avtalen",
                    TilsagnRequest::periodeSlutt,
                )
            }

            satsPeriodeStart.sats
        }
    }

    fun FieldValidator.validateBeregning(
        request: TilsagnBeregningRequest,
        valuta: Valuta,
        periode: Periode,
        avtalteSatser: List<AvtaltSats>,
    ): TilsagnBeregning {
        val sats = validateAvtaltSats(request.type, valuta, avtalteSatser, periode)
        val antallPlasser = validateAntallPlasser(request.type, request.antallPlasser)

        return when (request.type) {
            TilsagnBeregningType.FRI ->
                validateBeregningFriInput(valuta, request).bind()

            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED ->
                TilsagnBeregningFastSatsPerTiltaksplassPerManed.beregn(
                    TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                        periode = periode,
                        sats = sats,
                        antallPlasser = antallPlasser,
                    ),
                )

            TilsagnBeregningType.PRIS_PER_MANEDSVERK ->
                TilsagnBeregningPrisPerManedsverk.beregn(
                    TilsagnBeregningPrisPerManedsverk.Input(
                        periode = periode,
                        sats = sats,
                        antallPlasser = antallPlasser,
                        prisbetingelser = request.prisbetingelser,
                    ),
                )

            TilsagnBeregningType.PRIS_PER_HELE_UKESVERK ->
                TilsagnBeregningPrisPerHeleUkesverk.beregn(
                    TilsagnBeregningPrisPerHeleUkesverk.Input(
                        periode = periode,
                        sats = sats,
                        antallPlasser = antallPlasser,
                        prisbetingelser = request.prisbetingelser,
                    ),
                )

            TilsagnBeregningType.PRIS_PER_UKESVERK ->
                TilsagnBeregningPrisPerUkesverk.beregn(
                    TilsagnBeregningPrisPerUkesverk.Input(
                        periode = periode,
                        sats = sats,
                        antallPlasser = antallPlasser,
                        prisbetingelser = request.prisbetingelser,
                    ),
                )

            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING ->
                TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(
                    TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                        periode = periode,
                        sats = sats,
                        antallPlasser = antallPlasser,
                        prisbetingelser = request.prisbetingelser,
                        antallTimerOppfolgingPerDeltaker = validateAntallTimerOppfolgingPerDeltaker(
                            request.type,
                            request.antallTimerOppfolgingPerDeltaker,
                        ),
                    ),
                )
        }
    }

    private fun FieldValidator.validateAntallTimerOppfolgingPerDeltaker(
        type: TilsagnBeregningType,
        antallTimerOppfolgingPerDeltaker: Int?,
    ): Int = when (type) {
        TilsagnBeregningType.FRI,
        TilsagnBeregningType.PRIS_PER_MANEDSVERK,
        TilsagnBeregningType.PRIS_PER_UKESVERK,
        TilsagnBeregningType.PRIS_PER_HELE_UKESVERK,
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

    private fun FieldValidator.validateAntallPlasser(beregningType: TilsagnBeregningType, antallPlasser: Int?): Int = when (beregningType) {
        TilsagnBeregningType.FRI -> 0

        TilsagnBeregningType.PRIS_PER_MANEDSVERK,
        TilsagnBeregningType.PRIS_PER_UKESVERK,
        TilsagnBeregningType.PRIS_PER_HELE_UKESVERK,
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

    fun validateBeregningFriInput(prismodellValuta: Valuta, request: TilsagnBeregningRequest): Either<List<FieldError>, TilsagnBeregning> = validation {
        requireValid(!request.linjer.isNullOrEmpty()) {
            FieldError.of(
                "Du må legge til en linje",
                TilsagnRequest::beregning,
                TilsagnBeregningRequest::linjer,
            )
        }
        request.linjer.forEachIndexed { index, linje ->
            validate(linje.pris != null && linje.pris.belop > 0) {
                FieldError.ofPointer(
                    pointer = "/beregning/linjer/$index/pris/belop",
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

            validate(linje.pris?.valuta == prismodellValuta) {
                FieldError.ofPointer(
                    pointer = "/beregning/linjer/$index/pris/belop",
                    detail = "Må ha samme valuta som prismodellen: $prismodellValuta",
                )
            }
        }

        TilsagnBeregningFri.beregn(
            TilsagnBeregningFri.Input(
                linjer = request.linjer.map {
                    TilsagnBeregningFri.InputLinje(
                        id = it.id,
                        beskrivelse = it.beskrivelse!!,
                        pris = it.pris!!,
                        antall = it.antall!!,
                    )
                },
                prisbetingelser = request.prisbetingelser,
            ),
        )
    }
}
