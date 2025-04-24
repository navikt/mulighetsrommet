import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSetApentForPamelding(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: async (apentForPamelding: boolean) => {
      return GjennomforingerService.setApentForPamelding({
        path: { id: id },
        body: { apentForPamelding },
      });
    },

    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(id),
        }),
      ]);
    },

    throwOnError: true,
  });
}
