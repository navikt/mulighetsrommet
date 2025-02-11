import { useMutation } from "@tanstack/react-query";
import { DelutbetalingRequest, ProblemDetail, UtbetalingService } from "@mr/api-client-v2";

export function useOpprettDelutbetaling(utbetalingId: string) {
  return useMutation<unknown, ProblemDetail, DelutbetalingRequest>({
    mutationFn: (body: DelutbetalingRequest) =>
      UtbetalingService.opprettDelutbetaling({
        path: { id: utbetalingId },
        body,
      }),
  });
}
