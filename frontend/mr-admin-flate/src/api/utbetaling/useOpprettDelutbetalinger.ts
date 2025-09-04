import { useApiMutation } from "@/hooks/useApiMutation";
import {
  OpprettDelutbetalingerRequest,
  ProblemDetail,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";

export function useOpprettDelutbetalinger(utbetalingId: string) {
  return useApiMutation<unknown, ProblemDetail, OpprettDelutbetalingerRequest>({
    mutationFn: (body: OpprettDelutbetalingerRequest) =>
      UtbetalingService.opprettDelutbetalinger({
        path: { id: utbetalingId },
        body,
      }),
  });
}
