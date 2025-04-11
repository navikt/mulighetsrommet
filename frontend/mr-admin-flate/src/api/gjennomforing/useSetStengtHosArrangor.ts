import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import {
  GjennomforingerService,
  ProblemDetail,
  SetStengtHosArrangorRequest,
} from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSetStengtHosArrangor(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, SetStengtHosArrangorRequest>({
    mutationFn: async (data) => {
      return GjennomforingerService.setStengtHosArrangor({
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
