import { useApiMutation } from "@/hooks/useApiMutation";
import { ProblemDetail } from "@mr/api-client-v2";
import { OpprettUtbetalingRequest, UtbetalingService } from "@tiltaksadministrasjon/api-client";

export function useOpprettUtbetaling(utbetalingId: string) {
  return useApiMutation<unknown, ProblemDetail, OpprettUtbetalingRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettUtbetaling({
        path: { id: utbetalingId },
        body,
      }),
  });
}
