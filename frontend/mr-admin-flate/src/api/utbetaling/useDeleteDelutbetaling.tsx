import { useMutation } from "@tanstack/react-query";
import { ProblemDetail, UtbetalingService } from "@mr/api-client-v2";

export function useDeleteDelutbetaling(delutbetalingId: string) {
  return useMutation<unknown, ProblemDetail, unknown>({
    mutationFn: () => UtbetalingService.deleteDelutbetaling({ path: { id: delutbetalingId } }),
  });
}
