import {
  OpprettManuellUtbetalingkravRequest,
  ProblemDetail,
  UtbetalingService,
} from "@mr/api-client-v2";
import { useMutation } from "@tanstack/react-query";

export function useManueltUtbetalingskrav(kravId: string) {
  return useMutation<unknown, ProblemDetail, OpprettManuellUtbetalingkravRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettManuellUtbetalingKrav({
        path: { id: kravId },
        body,
      }),
  });
}
