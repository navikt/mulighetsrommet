import { useQueryClient } from "@tanstack/react-query";
import {
  GjennomforingRequest,
  GjennomforingService,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useCreateGjennomforing() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, GjennomforingRequest>({
    mutationFn: async (body: GjennomforingRequest) => {
      return GjennomforingService.createGjennomforing({ body });
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
