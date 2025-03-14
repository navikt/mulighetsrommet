import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";

export function useSetApentForPamelding(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
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
