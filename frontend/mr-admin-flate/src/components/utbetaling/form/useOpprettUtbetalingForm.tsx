import { OpprettUtbetalingRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { useRef } from "react";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useOpprettUtbetaling } from "@/api/utbetaling/mutations";

export function useOpprettUtbetalingForm(defaults: Partial<OpprettUtbetalingRequest>) {
  const navigate = useNavigate();

  const mutation = useOpprettUtbetaling();

  const utbetalingId = useRef(window.crypto.randomUUID());

  const form = useForm<OpprettUtbetalingRequest>({
    defaultValues: defaults,
  });

  function submit(data: OpprettUtbetalingRequest) {
    mutation.mutate(
      {
        ...data,
        id: utbetalingId.current,
        kidNummer: data.kidNummer || null,
      },
      {
        onSuccess: () => {
          form.reset();
          navigate(
            `/gjennomforinger/${defaults.gjennomforingId}/utbetalinger/${utbetalingId.current}`,
          );
        },
        onValidationError: (error: ValidationError) => {
          error.errors.forEach((error) => {
            const name = jsonPointerToFieldPath(error.pointer) as keyof OpprettUtbetalingRequest;
            form.setError(name, { type: "custom", message: error.detail });
          });
        },
      },
    );
  }

  return { form, onSubmit: form.handleSubmit(submit) };
}
