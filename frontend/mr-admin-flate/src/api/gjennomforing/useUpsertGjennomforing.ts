import { useQueryClient } from "@tanstack/react-query";
import {
  GjennomforingDetaljerDto,
  GjennomforingRequest,
  GjennomforingService,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertGjennomforing() {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: GjennomforingDetaljerDto }, ProblemDetail, GjennomforingRequest>({
    mutationFn: async (body: GjennomforingRequest) => {
      return GjennomforingService.upsertGjennomforing({ body });
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
