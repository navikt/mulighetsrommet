package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetaling
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object ArrangorflateUtbetalingValidator {
    const val MIN_ANTALL_VEDLEGG_OPPRETT_KRAV = 1

    fun maksUtbetalingsPeriodeSluttDato(
        gjennomforing: ArrangorflateTiltak,
        okonomiConfig: OkonomiConfig,
        relativeDate: LocalDate = LocalDate.now(),
    ): LocalDate {
        val opprettKravPeriodeSluttDato =
            okonomiConfig.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode]?.slutt
                ?: invalidGjennomforingOpprettKrav(gjennomforing)

        return when (gjennomforing.prismodell.type) {
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> minOf(relativeDate, opprettKravPeriodeSluttDato)

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> minOf(
                relativeDate.withDayOfMonth(1),
                opprettKravPeriodeSluttDato,
            )

            PrismodellType.ANNEN_AVTALT_PRIS,
            PrismodellType.ANNEN_AVTALT_PRIS_PER_DELTAKER,
            -> opprettKravPeriodeSluttDato

            PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            -> invalidGjennomforingOpprettKrav(gjennomforing)
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun invalidGjennomforingOpprettKrav(gjennomforing: ArrangorflateTiltak): Nothing {
        throw IllegalArgumentException("Kan ikke opprette utbetalingskrav for ${gjennomforing.tiltakstype.navn} med prismodell ${gjennomforing.prismodell.type.navn}")
    }

    fun validateOpprettKravArrangorflate(
        request: OpprettKravUtbetalingRequest,
        gjennomforing: ArrangorflateTiltak,
        okonomiConfig: OkonomiConfig,
        kontonummer: Kontonummer,
    ): Either<List<FieldError>, OpprettUtbetaling> = validation {
        val start = try {
            LocalDate.parse(request.periodeStart)
        } catch (_: DateTimeParseException) {
            null
        }
        validateNotNull(start) {
            FieldError.of(
                "Dato må være på formatet 'yyyy-mm-dd'",
                OpprettKravUtbetalingRequest::periodeStart,
            )
        }
        val slutt = try {
            LocalDate.parse(request.periodeSlutt)
        } catch (_: DateTimeParseException) {
            null
        }
        validateNotNull(slutt) {
            FieldError.of(
                "Dato må være på formatet 'yyyy-mm-dd'",
                OpprettKravUtbetalingRequest::periodeSlutt,
            )
        }
        requireValid(start != null && slutt != null)

        validate(start.isBefore(slutt)) {
            FieldError.of(
                "Periodeslutt må være etter periodestart",
                OpprettKravUtbetalingRequest::periodeStart,
            )
        }

        validate(!slutt.isAfter(maksUtbetalingsPeriodeSluttDato(gjennomforing, okonomiConfig))) {
            FieldError.of(
                "Du kan ikke sende inn for valgt periode før perioden er passert",
                OpprettKravUtbetalingRequest::periodeSlutt,
            )
        }

        validate(request.belop > 0) {
            FieldError.of("Beløp må være positivt", OpprettKravUtbetalingRequest::belop)
        }
        validate(request.vedlegg.size >= MIN_ANTALL_VEDLEGG_OPPRETT_KRAV) {
            FieldError.of("Du må legge ved vedlegg", OpprettKravUtbetalingRequest::vedlegg)
        }
        requireValid(request.kidNummer == null || Kid.parse(request.kidNummer) != null) {
            FieldError.of(
                "Ugyldig kid",
                OpprettKravUtbetalingRequest::kidNummer,
            )
        }

        OpprettUtbetaling(
            gjennomforingId = gjennomforing.id,
            periodeStart = LocalDate.parse(request.periodeStart),
            periodeSlutt = LocalDate.parse(request.periodeSlutt),
            pris = request.belop.withValuta(gjennomforing.prismodell.valuta),
            kontonummer = kontonummer,
            kidNummer = request.kidNummer?.let { Kid.parseOrThrow(it) },
            vedlegg = request.vedlegg,
        )
    }
}
