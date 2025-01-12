import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, AvbrytGjennomforingAarsak, GjennomforingerService } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytGjennomforing() {
  const client = useQueryClient();

  return useMutation<unknown, ApiError, { id: string; aarsak: AvbrytGjennomforingAarsak | string }>(
    {
      mutationFn: (data: { id: string; aarsak?: AvbrytGjennomforingAarsak | string }) => {
        return GjennomforingerService.avbrytGjennomforing({
          id: data.id,
          requestBody: { aarsak: data.aarsak },
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
    },
  );
}
