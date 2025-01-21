import { useMutation, useQueryClient } from "@tanstack/react-query";
import { GjennomforingerService, GjennomforingRequest } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertGjennomforing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: GjennomforingRequest) =>
      GjennomforingerService.upsertGjennomforing({
        body,
      }),

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
