import {
  AarsakerOgForklaringRequestDelutbetalingReturnertAarsak,
  OpprettDelutbetalingerRequest,
  OpprettUtbetalingRequest,
  ProblemDetail,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";
import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "@/hooks/useApiMutation";
import { QueryKeys } from "@/api/QueryKeys";

export function useAttesterDelutbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.attesterDelutbetaling({ path: { id } }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
    },
  });
}

export function useOpprettDelutbetalinger(utbetalingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, OpprettDelutbetalingerRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettDelutbetalinger({ path: { id: utbetalingId }, body }),
    async onSuccess() {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling(utbetalingId) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.utbetalingsLinjer(utbetalingId) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.utbetalingHistorikk(utbetalingId) }),
      ]);
    },
  });
}

export function useOpprettUtbetaling(utbetalingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, OpprettUtbetalingRequest>({
    mutationFn: (body) => UtbetalingService.opprettUtbetaling({ path: { id: utbetalingId }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetalingerByGjennomforing() });
    },
  });
}

export function useReturnerDelutbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: AarsakerOgForklaringRequestDelutbetalingReturnertAarsak }
  >({
    mutationFn: ({ id, body }) => UtbetalingService.returnerDelutbetaling({ path: { id }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
    },
  });
}

export function useSlettKorreksjon() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.slettKorreksjon({ path: { id } }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
    },
  });
}
