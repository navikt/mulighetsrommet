import {
  ArrangorReferanseType,
  BBan,
  UtbetalingDto,
  UtbetalingRequest,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { useForm } from "react-hook-form";
import { useRedigerUtbetaling } from "@/api/utbetaling/mutations";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { applyValidationErrors } from "@/components/skjema/helpers";

interface UseRedigerUtbetalingFormOptions {
  onSuccess?: () => void;
}

export function useRedigerUtbetalingForm(
  utbetaling: UtbetalingDto,
  options?: UseRedigerUtbetalingFormOptions,
) {
  const defaults = {
    id: utbetaling.id,
    gjennomforingId: utbetaling.gjennomforingId,
    periodeStart: utbetaling.periode.start,
    periodeSlutt: yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 })) ?? null,
    journalpostId: utbetaling.journalpostId,
    kommentar: utbetaling.kommentar,
    pris: utbetaling.beregning,
    utbetalingsDato: utbetaling.utbetalesTidligstDato ?? null,
    korrigererUtbetaling: utbetaling.korreksjon?.opprinneligUtbetaling,
    korreksjonBegrunnelse: utbetaling.korreksjon?.begrunnelse,
    ...betalingsReferanseDefaults(utbetaling),
  };

  const form = useForm<UtbetalingRequest>({ defaultValues: defaults });

  const mutation = useRedigerUtbetaling();

  function submit(data: UtbetalingRequest) {
    mutation.mutate(emptyDatesToNull(data), {
      onSuccess: options?.onSuccess,
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
    });
  }

  return { form, submit, mutation };
}

function betalingsReferanseDefaults(
  utbetaling: UtbetalingDto,
): Pick<UtbetalingRequest, "referanse" | "kidNummer" | "fakturanummer"> {
  const bban: BBan | null =
    utbetaling.betalingsinformasjon?.type === "BBan" ? utbetaling.betalingsinformasjon : null;

  if (!bban?.referanse) {
    return {
      referanse: null,
      kidNummer: null,
      fakturanummer: null,
    };
  }

  switch (bban.referanse.type) {
    case "KidNummer":
      return {
        referanse: ArrangorReferanseType.KID,
        kidNummer: bban.referanse.kid,
        fakturanummer: null,
      };
    case "MeldingTilMottaker":
      return {
        referanse: ArrangorReferanseType.FAKTURANUMMER,
        kidNummer: null,
        fakturanummer: bban.referanse.melding,
      };
  }
}

// Unngå deserialiseringsproblemer: default verdi er tom streng, mens api krever null
function emptyDatesToNull(data: UtbetalingRequest) {
  return {
    ...data,
    periodeStart: data.periodeStart ? data.periodeStart : null,
    periodeSlutt: data.periodeSlutt ? data.periodeSlutt : null,
    utbetalingsDato: data.utbetalingsDato ? data.utbetalingsDato : null,
  };
}
