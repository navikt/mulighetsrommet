import { useMutation } from "@tanstack/react-query";
import { ProblemDetail, UtbetalingRequest, UtbetalingService } from "@mr/api-client-v2";

export function useOpprettUtbetaling(kravId: string) {
  return useMutation<unknown, ProblemDetail, UtbetalingRequest>({
    mutationFn: (body: UtbetalingRequest) =>
      UtbetalingService.opprettUtbetaling({
        path: { id: kravId },
        body,
      }),
  });
}
