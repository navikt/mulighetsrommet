import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingService } from "@tiltaksadministrasjon/api-client";

export function useSetPublisert(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { publisert: boolean }) => {
      return GjennomforingService.setPublisert({
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
  });
}
