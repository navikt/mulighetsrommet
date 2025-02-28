import { ProblemDetail, UtbetalingService, DelutbetalingBulkRequest } from "@mr/api-client-v2";
import { useMutation } from "@tanstack/react-query";

export function useUpsertDelutbetalingBulk(utbetalingId: string) {
  return useMutation<unknown, ProblemDetail, DelutbetalingBulkRequest>({
    mutationFn: (body: DelutbetalingBulkRequest) =>
      UtbetalingService.upsertDelutbetalingBulk({
        path: { id: utbetalingId },
        body,
      }),
  });
}
