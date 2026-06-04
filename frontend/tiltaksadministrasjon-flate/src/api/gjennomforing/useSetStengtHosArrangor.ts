import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import {
  GjennomforingService,
  ProblemDetail,
  SetStengtHosArrangorRequest,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSetStengtHosArrangor(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, SetStengtHosArrangorRequest>({
    mutationFn: async (data) => {
      return GjennomforingService.setStengtHosArrangor({
        path: { id: gjennomforingId },
        body: data,
      });
    },

    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.gjennomforing(gjennomforingId),
      });
    },
  });
}
