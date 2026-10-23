import {
  AarsakerOgBegrunnelseRequestTilskuddBehandlingStatusAarsak,
  ProblemDetail,
  TilskuddBehandlingOpphorResponse,
  TilskuddBehandlingRequest,
  TilskuddBehandlingService,
  UtbetalingService,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "@/hooks/useApiMutation";
import { QueryKeys } from "@/api/QueryKeys";
import { getValidationError } from "@/utils/Utils";

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

async function validateWithMutation(
  mutationFn: () => Promise<unknown>,
): Promise<ValidationError | undefined> {
  try {
    await mutationFn();
    return undefined;
  } catch (error) {
    const validationError = getValidationError(error);
    if (validationError?.status === 400) return validationError;
    throw error;
  }
}

export function useValidateSaksopplysninger() {
  return useMutation<ValidationError | undefined, unknown, TilskuddBehandlingRequest>({
    mutationFn: (body) =>
      validateWithMutation(() =>
        TilskuddBehandlingService.validerSaksopplysningerTilskuddsBehandling({ body }),
      ),
  });
}

export function useValidateTilskuddBehandling() {
  return useMutation<ValidationError | undefined, unknown, TilskuddBehandlingRequest>({
    mutationFn: (body) =>
      validateWithMutation(() => TilskuddBehandlingService.validerTilskuddsBehandling({ body })),
  });
}

export function useGodkjennTilskuddBehandling(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, string>({
    mutationFn: (id) => TilskuddBehandlingService.godkjennTilskuddBehandling({ path: { id } }),
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
    { id: string; body: AarsakerOgBegrunnelseRequestTilskuddBehandlingStatusAarsak }
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

export function useOpphorBrukerUtbetaling() {
  const queryClient = useQueryClient();

  return useApiMutation<
    TilskuddBehandlingOpphorResponse,
    ProblemDetail,
    { tilskuddBehandlingId: string; tilskuddVedtakId: string }
  >({
    mutationFn: async ({ tilskuddBehandlingId, tilskuddVedtakId }) =>
      (
        await UtbetalingService.postTilskuddVedtakOpphor({
          path: { tilskuddBehandlingId, tilskuddVedtakId },
        })
      ).data,
    async onSuccess(data) {
      await queryClient.invalidateQueries({
        queryKey: QueryKeys.tilskuddBehandling(data.behandlingId),
      });
    },
  });
}
