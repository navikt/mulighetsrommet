import { useApiMutation } from "@/hooks/useApiMutation";
import {
  OpprettUtbetalingRequest,
  ProblemDetail,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";

export function useOpprettUtbetaling(utbetalingId: string) {
  return useApiMutation<unknown, ProblemDetail, OpprettUtbetalingRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettUtbetaling({
        path: { id: utbetalingId },
        body,
      }),
  });
}
