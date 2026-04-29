import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import {
  GjennomforingService,
  SetEstimertVentetidRequest,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSetEstimertVentetid(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: async (body: SetEstimertVentetidRequest) => {
      return GjennomforingService.setEstimertVentetid({
        path: { id },
        body,
      });
    },

    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: QueryKeys.gjennomforing(id) });
    },
  });
}
