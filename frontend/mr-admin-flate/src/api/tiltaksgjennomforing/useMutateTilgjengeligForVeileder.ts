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

    onSuccess: async (_, request) => {
      await queryClient.invalidateQueries({
        queryKey: QueryKeys.tiltaksgjennomforing(request.id),
      });
    },

    throwOnError: true,
  });
}
