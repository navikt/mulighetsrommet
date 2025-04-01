import { useMutation } from "@tanstack/react-query";
import { BesluttDelutbetalingRequest, ProblemDetail, UtbetalingService } from "@mr/api-client-v2";

export function useBesluttDelutbetaling() {
  return useMutation<unknown, ProblemDetail, { id: string; body: BesluttDelutbetalingRequest }>({
    mutationFn: ({ id, body }: { id: string; body: BesluttDelutbetalingRequest }) =>
      UtbetalingService.besluttDelutbetaling({ path: { id }, body }),
  });
}
