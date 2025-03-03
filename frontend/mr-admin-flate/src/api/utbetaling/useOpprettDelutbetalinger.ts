import { ProblemDetail, UtbetalingService, OpprettDelutbetalingerRequest } from "@mr/api-client-v2";
import { useMutation } from "@tanstack/react-query";

export function useOpprettDelutbetalinger(utbetalingId: string) {
  return useMutation<unknown, ProblemDetail, OpprettDelutbetalingerRequest>({
    mutationFn: (body: OpprettDelutbetalingerRequest) =>
      UtbetalingService.opprettDelutbetalinger({
        path: { id: utbetalingId },
        body,
      }),
  });
}
