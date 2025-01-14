import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client";

export function useMutatePublisert() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { id: string; publisert: boolean }) => {
      return GjennomforingerService.setPublisert({
        id: data.id,
        requestBody: { publisert: data.publisert },
      });
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(request.id),
        }),
      ]);
    },

    throwOnError: true,
  });
}
