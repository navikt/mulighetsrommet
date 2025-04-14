import { useApiMutation } from "@/hooks/useApiMutation";
import {
  OpprettManuellUtbetalingRequest,
  ProblemDetail,
  UtbetalingService,
} from "@mr/api-client-v2";

export function useCreateManuellUtbetaling(utbetalingId: string) {
  return useApiMutation<unknown, ProblemDetail, OpprettManuellUtbetalingRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettManuellUtbetaling({
        path: { id: utbetalingId },
        body,
      }),
  });
}
