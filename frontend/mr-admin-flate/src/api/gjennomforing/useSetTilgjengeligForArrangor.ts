import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { ApiError, GjennomforingerService } from "@mr/api-client";

export function useSetTilgjengeligForArrangor() {
  const queryClient = useQueryClient();

  return useMutation<unknown, ApiError, { id: string; tilgjengeligForArrangorDato: string }>({
    mutationFn: async (data) => {
      return GjennomforingerService.setTilgjengeligForArrangor({
        id: data.id,
        requestBody: { tilgjengeligForArrangorDato: data.tilgjengeligForArrangorDato },
      });
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(request.id),
        }),
      ]);
    },
  });
}
