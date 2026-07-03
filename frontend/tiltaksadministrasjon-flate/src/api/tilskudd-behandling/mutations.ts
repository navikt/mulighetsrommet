import {
  AarsakerOgForklaringRequestTilskuddBehandlingStatusAarsak,
  ProblemDetail,
  TilskuddBehandlingRequest,
  TilskuddBehandlingService,
} from "@tiltaksadministrasjon/api-client";
import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "@/hooks/useApiMutation";
import { QueryKeys } from "@/api/QueryKeys";

export function useOpprettTilskuddBehandling(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, TilskuddBehandlingRequest>({
    mutationFn: (body) => TilskuddBehandlingService.opprettTilskuddBehandling({ body }),
    async onSuccess() {
      await queryClient.invalidateQueries({
        queryKey: QueryKeys.tilskuddBehandlinger(gjennomforingId),
      });
    },
  });
}

export function useVedtaksbrevPdfBlobPost() {
  return useApiMutation<Blob, ProblemDetail, TilskuddBehandlingRequest>({
    mutationFn: async (body) => {
      const result = await TilskuddBehandlingService.postTilskuddBehandlingVedtaksbrevPdf({ body });
      return result.data as Blob;
    },
  });
}

export function useGodkjennTilskuddBehandling(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, string>({
    mutationFn: (id) => TilskuddBehandlingService.attesterTilskuddBehandling({ path: { id } }),
    async onSuccess() {
      await queryClient.invalidateQueries({
        queryKey: QueryKeys.tilskuddBehandlinger(gjennomforingId),
      });
    },
  });
}

export function useReturnerTilskuddBehandling(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: AarsakerOgForklaringRequestTilskuddBehandlingStatusAarsak }
  >({
    mutationFn: ({ id, body }) =>
      TilskuddBehandlingService.returnerTilskuddBehandling({ path: { id }, body }),
    async onSuccess() {
      await queryClient.invalidateQueries({
        queryKey: QueryKeys.tilskuddBehandlinger(gjennomforingId),
      });
    },
  });
}
