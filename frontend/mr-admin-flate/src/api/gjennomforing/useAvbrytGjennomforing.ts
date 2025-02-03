import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AvbrytGjennomforingAarsak,
  GjennomforingerService,
  ProblemDetail,
} from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytGjennomforing() {
  const client = useQueryClient();

  return useMutation<
    unknown,
    ProblemDetail,
    { id: string; aarsak?: AvbrytGjennomforingAarsak | string }
  >({
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
