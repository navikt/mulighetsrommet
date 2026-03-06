package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateOpprettUtbetaling
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object ArrangorflateUtbetalingValidator {
    const val MIN_ANTALL_VEDLEGG_OPPRETT_KRAV = 1

    fun maksUtbetalingsPeriodeSluttDato(
        prismodell: PrismodellType,
        periode: Periode?,
        today: LocalDate = LocalDate.now(),
    ): LocalDate {
        val opprettKravPeriodeSluttDato = periode?.slutt ?: invalidGjennomforingOpprettKrav(prismodell)

        return when (prismodell) {
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
            -> minOf(today, opprettKravPeriodeSluttDato)

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            -> minOf(today.withDayOfMonth(1), opprettKravPeriodeSluttDato)

            PrismodellType.ANNEN_AVTALT_PRIS,
            -> opprettKravPeriodeSluttDato

            PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            -> invalidGjennomforingOpprettKrav(prismodell)
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun invalidGjennomforingOpprettKrav(prismodell: PrismodellType): Nothing {
        throw IllegalArgumentException("Kan ikke opprette utbetalingskrav for $prismodell")
    }

    data class ValidateOpprettUtbetalingContext(
        val gjennomforingId: UUID,
        val tiltakskode: Tiltakskode,
        val prismodell: PrismodellType,
        val valuta: Valuta,
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
    )

    fun validateOpprettKravArrangorflate(
        ctx: ValidateOpprettUtbetalingContext,
        request: OpprettKravUtbetalingRequest,
    ): Either<List<FieldError>, ArrangorflateOpprettUtbetaling> = validation {
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

        val maksSluttdato = maksUtbetalingsPeriodeSluttDato(
            prismodell = ctx.prismodell,
            periode = ctx.gyldigTilsagnPeriode[ctx.tiltakskode],
        )
        validate(!slutt.isAfter(maksSluttdato)) {
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

        ArrangorflateOpprettUtbetaling(
            gjennomforingId = ctx.gjennomforingId,
            periode = Periode(LocalDate.parse(request.periodeStart), LocalDate.parse(request.periodeSlutt)),
            pris = request.belop.withValuta(ctx.valuta),
            kidNummer = request.kidNummer?.let { Kid.parseOrThrow(it) },
            vedlegg = request.vedlegg,
        )
    }
}
