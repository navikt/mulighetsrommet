import { OpprettUtbetalingRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { useRef } from "react";
import { useOpprettUtbetaling } from "@/api/utbetaling/mutations";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function useOpprettUtbetalingForm(defaults: Partial<OpprettUtbetalingRequest>) {
  const navigate = useNavigate();

  const mutation = useOpprettUtbetaling();

  const utbetalingId = useRef(window.crypto.randomUUID());

  const form = useForm<OpprettUtbetalingRequest>({
    defaultValues: defaults,
  });

  function submit(data: OpprettUtbetalingRequest) {
    mutation.mutate(
      { ...data, id: utbetalingId.current },
      {
        onSuccess: () => {
          navigate(
            `/gjennomforinger/${defaults.gjennomforingId}/utbetalinger/${utbetalingId.current}`,
          );
        },
        onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
      },
    );
  }

  return { form, submit };
}
