import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import {
  GjennomforingService,
  ProblemDetail,
  SetTilgjengligForArrangorRequest,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSetTilgjengeligForArrangor(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, SetTilgjengligForArrangorRequest>({
    mutationFn: async (body) => {
      return GjennomforingService.setTilgjengeligForArrangor({
        path: { id },
        body,
      });
    },

    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.gjennomforing(id),
      });
    },
  });
}
