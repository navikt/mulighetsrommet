import { client } from "@tiltaksadministrasjon/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

export function useSetPublisertTiltakBeskrivelse(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { publisert: boolean }) => {
      return client.put({
        url: "/api/tiltaksadministrasjon/tiltak-beskrivelser/{id}/tilgjengelig-for-veileder",
        path: { id },
        body: { publisert: data.publisert },
      });
    },
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltakBeskrivelser(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltakBeskrivelse(id),
        }),
      ]);
    },
  });
}
