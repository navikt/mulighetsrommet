import {
  AarsakerOgForklaringRequestDelutbetalingReturnertAarsak,
  OpprettDelutbetalingerRequest,
  OpprettUtbetalingRequest,
  ProblemDetail,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useAttesterDelutbetaling() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.attesterDelutbetaling({ path: { id } }),
  });
}

export function useOpprettDelutbetalinger(utbetalingId: string) {
  return useApiMutation<unknown, ProblemDetail, OpprettDelutbetalingerRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettDelutbetalinger({ path: { id: utbetalingId }, body }),
  });
}

export function useOpprettUtbetaling(utbetalingId: string) {
  return useApiMutation<unknown, ProblemDetail, OpprettUtbetalingRequest>({
    mutationFn: (body) => UtbetalingService.opprettUtbetaling({ path: { id: utbetalingId }, body }),
  });
}

export function useReturnerDelutbetaling() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: AarsakerOgForklaringRequestDelutbetalingReturnertAarsak }
  >({
    mutationFn: ({ id, body }) => UtbetalingService.returnerDelutbetaling({ path: { id }, body }),
  });
}

export function useSlettKorreksjon() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.slettKorreksjon({ path: { id } }),
  });
}
