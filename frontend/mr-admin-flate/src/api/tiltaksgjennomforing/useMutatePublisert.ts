import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useMutatePublisert() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { id: string; publisert: boolean }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.setPublisert({
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
