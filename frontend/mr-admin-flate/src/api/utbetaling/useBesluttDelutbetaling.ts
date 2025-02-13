import { useMutation } from "@tanstack/react-query";
import { BesluttDelutbetalingRequest, ProblemDetail, UtbetalingService } from "@mr/api-client-v2";

export function useBesluttDelutbetaling(utbetalingId: string) {
  return useMutation<unknown, ProblemDetail, BesluttDelutbetalingRequest>({
    mutationFn: (body: BesluttDelutbetalingRequest) =>
      UtbetalingService.besluttDeltbetaling({ path: { id: utbetalingId }, body }),
  });
}
