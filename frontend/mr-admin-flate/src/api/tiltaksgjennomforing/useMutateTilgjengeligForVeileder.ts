import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useMutateTilgjengeligForVeileder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { id: string; tilgjengeligForVeileder: boolean }) => {
      return mulighetsrommetClient.tiltaksgjennomforinger.setTilgjengeligForVeileder({
        id: data.id,
        requestBody: { tilgjengeligForVeileder: data.tilgjengeligForVeileder },
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
