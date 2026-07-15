import { client } from "@tiltaksadministrasjon/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

export function useSetPublisertIndividuellGjennomforing(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { publisert: boolean }) => {
      return client.put({
        url: "/api/tiltaksadministrasjon/individuelle-gjennomforinger/{id}/tilgjengelig-for-veileder",
        path: { id },
        body: { publisert: data.publisert },
      });
    },
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.individuelleGjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.individuellGjennomforing(id),
        }),
      ]);
    },
  });
}
