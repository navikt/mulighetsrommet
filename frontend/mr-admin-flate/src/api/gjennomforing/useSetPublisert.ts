import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";

export function useSetPublisert(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { publisert: boolean }) => {
      return GjennomforingerService.setPublisert({
        path: { id },
        body: { publisert: data.publisert },
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
