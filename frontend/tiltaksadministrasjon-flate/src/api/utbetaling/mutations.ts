import {
  AarsakerOgBegrunnelseRequestUtbetalingLinjeReturnertAarsak,
  AarsakerOgBegrunnelseRequestUtbetalingStatusAarsak,
  OpprettUtbetalingLinjerRequest,
  ProblemDetail,
  UtbetalingRequest,
  UtbetalingService,
} from "@tiltaksadministrasjon/api-client";
import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "@/hooks/useApiMutation";
import { QueryKeys } from "@/api/QueryKeys";

export function useGodkjennUtbetalingLinje() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.godkjennUtbetalingLinje({ path: { id } }),
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
    { id: string; body: AarsakerOgBegrunnelseRequestUtbetalingLinjeReturnertAarsak }
  >({
    mutationFn: ({ id, body }) => UtbetalingService.returnerUtbetalingLinje({ path: { id }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling() });
    },
  });
}

export function useAvbrytUtbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: AarsakerOgBegrunnelseRequestUtbetalingStatusAarsak }
  >({
    mutationFn: ({ id, body }) => UtbetalingService.avbrytUtbetaling({ path: { id }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling() });
    },
  });
}

export function useGodkjennAvbrytelseUtbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.godkjennAvbrytelseUtbetaling({ path: { id } }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling() });
    },
  });
}

export function useAvslaAvbrytelseUtbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: AarsakerOgBegrunnelseRequestUtbetalingStatusAarsak }
  >({
    mutationFn: ({ id, body }) =>
      UtbetalingService.avslaAvbrytelseUtbetaling({ path: { id }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.utbetaling() });
    },
  });
}

export function useSlettUtbetaling() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => UtbetalingService.slettUtbetaling({ path: { id } }),
  });
}
