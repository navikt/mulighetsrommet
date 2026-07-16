import { client } from "@tiltaksadministrasjon/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

export function useSetPublisertTiltakDokument(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { publisert: boolean }) => {
      return client.put({
        url: "/api/tiltaksadministrasjon/tiltak-dokumenter/{id}/tilgjengelig-for-veileder",
        path: { id },
        body: { publisert: data.publisert },
      });
    },
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltakDokumenter(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.tiltakDokument(id),
        }),
      ]);
    },
  });
}
