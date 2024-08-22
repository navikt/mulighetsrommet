import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingerService } from "@mr/api-client";

export function useMutatePublisert() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { id: string; publisert: boolean }) => {
      return TiltaksgjennomforingerService.setPublisert({
        id: data.id,
        requestBody: { publisert: data.publisert },
      });
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltaksgjennomforing(request.id),
        }),
      ]);
    },

    throwOnError: true,
  });
}
