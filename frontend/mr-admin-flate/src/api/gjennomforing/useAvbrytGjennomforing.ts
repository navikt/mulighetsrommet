import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AvbrytGjennomforingAarsak, GjennomforingerService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytGjennomforing() {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (data: { id: string; aarsak?: AvbrytGjennomforingAarsak | string }) => {
      return GjennomforingerService.avbrytGjennomforing({
        path: { id: data.id },
        body: { aarsak: data.aarsak },
      });
    },
    onSuccess(_, request) {
      return Promise.all([
        client.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(request.id),
        }),
        client.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
      ]);
    },
  });
}
