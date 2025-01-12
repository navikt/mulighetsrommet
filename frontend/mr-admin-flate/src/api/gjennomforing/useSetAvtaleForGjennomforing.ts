import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client";

export function useSetAvtaleForGjennomforing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { gjennomforingId: string; avtaleId?: string }) => {
      return GjennomforingerService.setAvtaleForGjennomforing({
        id: data.gjennomforingId,
        requestBody: {
          avtaleId: data.avtaleId,
        },
      });
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(request.gjennomforingId),
        }),
      ]);
    },
  });
}
