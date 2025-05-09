import { useQueryClient } from "@tanstack/react-query";
import {
  GjennomforingDto,
  GjennomforingerService,
  GjennomforingRequest,
  ProblemDetail,
} from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertGjennomforing() {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: GjennomforingDto }, ProblemDetail, GjennomforingRequest>({
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
