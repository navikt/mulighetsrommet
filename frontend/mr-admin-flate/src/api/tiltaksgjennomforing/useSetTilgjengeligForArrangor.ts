import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { ApiError, TiltaksgjennomforingerService } from "mulighetsrommet-api-client";

export function useSetTilgjengeligForArrangor() {
  const queryClient = useQueryClient();

  return useMutation<unknown, ApiError, { id: string; tilgjengeligForArrangorDato: string }>({
    mutationFn: async (data) => {
      return TiltaksgjennomforingerService.setTilgjengeligForArrangor({
        id: data.id,
        requestBody: { tilgjengeligForArrangorDato: data.tilgjengeligForArrangorDato },
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
  });
}
