import { useApiMutation } from "@/hooks/useApiMutation";
import { ProblemDetail, UtbetalingService, OpprettDelutbetalingerRequest } from "@mr/api-client-v2";

export function useOpprettDelutbetalinger(utbetalingId: string) {
  return useApiMutation<unknown, ProblemDetail, OpprettDelutbetalingerRequest>({
    mutationFn: (body: OpprettDelutbetalingerRequest) =>
      UtbetalingService.opprettDelutbetalinger({
        path: { id: utbetalingId },
        body,
      }),
  });
}
