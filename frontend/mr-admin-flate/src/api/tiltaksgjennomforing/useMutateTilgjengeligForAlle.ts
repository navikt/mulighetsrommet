import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useMutateTilgjengeligForAlle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { id: string; tilgjengeligForAlle: boolean }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.setTilgjengeligForAlle({
        id: data.id,
        requestBody: { tilgjengeligForAlle: data.tilgjengeligForAlle },
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
