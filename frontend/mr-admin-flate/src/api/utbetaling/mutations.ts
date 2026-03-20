import {
  AarsakerOgForklaringRequestUtbetalingLinjeReturnertAarsak,
  OpprettUtbetalingLinjerRequest,
  ProblemDetail,
  UtbetalingRequest,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";
import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "@/hooks/useApiMutation";
import { QueryKeys } from "@/api/QueryKeys";

export function useAttesterUtbetalingLinje() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.attesterUtbetalingLinje({ path: { id } }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling() });
    },
  });
}

export function useOpprettUtbetalingLinjer(utbetalingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, OpprettUtbetalingLinjerRequest>({
    mutationFn: (body) =>
      UtbetalingService.opprettUtbetalingLinjer({ path: { id: utbetalingId }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling(utbetalingId) });
    },
  });
}

export function useOpprettUtbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, UtbetalingRequest>({
    mutationFn: (body) => UtbetalingService.opprettUtbetaling({ body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetalingerByGjennomforing() });
    },
  });
}

export function useRedigerUtbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, UtbetalingRequest>({
    mutationFn: (body) => UtbetalingService.redigerUtbetaling({ body }),
    async onSuccess(_, request) {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling(request.id) });
    },
  });
}

export function useReturnerUtbetalingLinje() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: AarsakerOgForklaringRequestUtbetalingLinjeReturnertAarsak }
  >({
    mutationFn: ({ id, body }) => UtbetalingService.returnerUtbetalingLinje({ path: { id }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling() });
    },
  });
}

export function useSlettKorreksjon() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.slettKorreksjon({ path: { id } }),
  });
}
