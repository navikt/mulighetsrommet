import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  ProblemDetail,
  TilsagnRequest,
  TilsagnService,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";
import { QueryKeys } from "@/api/QueryKeys";

export function useGodkjennTilsagn() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => TilsagnService.godkjennTilsagn({ path: { id } }),
    mutationKey: QueryKeys.godkjennTilsagn(),
  });
}

export function useOpprettTilsagn() {
  return useApiMutation<unknown, ProblemDetail, TilsagnRequest>({
    mutationFn: (body) => TilsagnService.opprettTilsagn({ body }),
    mutationKey: QueryKeys.opprettTilsagn(),
  });
}

export function useReturnerTilsagn() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) =>
      TilsagnService.returnerTilsagn({ path: { id }, body: request }),
    mutationKey: QueryKeys.returnerTilsagn(),
  });
}

export function useTilsagnTilAnnullering() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) => TilsagnService.tilAnnullering({ path: { id }, body: request }),
    mutationKey: QueryKeys.annullerTilsagn(),
  });
}

export function useTilsagnTilOppgjor() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) => TilsagnService.gjorOpp({ path: { id }, body: request }),
    mutationKey: QueryKeys.gjorOppTilsagn(),
  });
}

export function useSlettTilsagn() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => TilsagnService.slettTilsagn({ path: { id } }),
    mutationKey: QueryKeys.slettTilsagn(),
  });
}
