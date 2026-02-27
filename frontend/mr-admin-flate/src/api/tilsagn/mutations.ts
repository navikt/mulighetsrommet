import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  ProblemDetail,
  TilsagnRequest,
  TilsagnService,
} from "@tiltaksadministrasjon/api-client";
import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "@/hooks/useApiMutation";
import { QueryKeys } from "@/api/QueryKeys";

export function useGodkjennTilsagn() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => TilsagnService.godkjennTilsagn({ path: { id } }),
    mutationKey: QueryKeys.godkjennTilsagn(),
    async onSuccess(_, { id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.getTilsagn(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnHistorikk(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnRequest(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.getAllTilsagn() }),
      ]);
    },
  });
}

export function useOpprettTilsagn() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, TilsagnRequest>({
    mutationFn: (body) => TilsagnService.opprettTilsagn({ body }),
    mutationKey: QueryKeys.opprettTilsagn(),
    async onSuccess() {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.getAllTilsagn() });
    },
  });
}

export function useReturnerTilsagn() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) =>
      TilsagnService.returnerTilsagn({ path: { id }, body: request }),
    mutationKey: QueryKeys.returnerTilsagn(),
    async onSuccess(_, { id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.getTilsagn(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnHistorikk(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnRequest(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.getAllTilsagn() }),
      ]);
    },
  });
}

export function useTilsagnTilAnnullering() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) => TilsagnService.tilAnnullering({ path: { id }, body: request }),
    mutationKey: QueryKeys.annullerTilsagn(),
    async onSuccess(_, { id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.getTilsagn(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnHistorikk(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnRequest(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.getAllTilsagn() }),
      ]);
    },
  });
}

export function useTilsagnTilOppgjor() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) => TilsagnService.gjorOpp({ path: { id }, body: request }),
    mutationKey: QueryKeys.gjorOppTilsagn(),
    async onSuccess(_, { id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.getTilsagn(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnHistorikk(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnRequest(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.getAllTilsagn() }),
      ]);
    },
  });
}

export function useSlettTilsagn() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => TilsagnService.slettTilsagn({ path: { id } }),
    mutationKey: QueryKeys.slettTilsagn(),
    async onSuccess(_, { id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.getTilsagn(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnHistorikk(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.tilsagnRequest(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.getAllTilsagn() }),
      ]);
    },
  });
}
