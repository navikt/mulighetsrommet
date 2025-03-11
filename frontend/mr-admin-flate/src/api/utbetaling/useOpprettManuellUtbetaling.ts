import {
  OpprettManuellUtbetalingRequest,
  ProblemDetail,
  UtbetalingService,
} from "@mr/api-client-v2";
import { useMutation } from "@tanstack/react-query";

export function useCreateManuellUtbdetaling(utbetalingId: string) {
  return useMutation<unknown, ProblemDetail, OpprettManuellUtbetalingRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettManuellUtbetaling({
        path: { id: utbetalingId },
        body,
      }),
  });
}
