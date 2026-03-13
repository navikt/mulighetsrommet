import {
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
    kidNummer:
      utbetaling.betalingsinformasjon?.type === "BBan" ? utbetaling.betalingsinformasjon.kid : null,
    pris: utbetaling.beregning,
    korrigererUtbetaling: utbetaling.korreksjon?.opprinneligUtbetaling,
    korreksjonBegrunnelse: utbetaling.korreksjon?.begrunnelse,
  };

  const form = useForm<UtbetalingRequest>({ defaultValues: defaults });

  const mutation = useRedigerUtbetaling();

  function submit(data: UtbetalingRequest) {
    mutation.mutate(data, {
      onSuccess: options?.onSuccess,
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
    });
  }

  return { form, submit, mutation };
}
