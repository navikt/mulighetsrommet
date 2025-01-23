import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService, SetStengtHosArrangorRequest } from "@mr/api-client-v2";
import { ApiError } from "@mr/frontend-common/components/error-handling/errors";

export function useSetStengtHosArrangor(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useMutation<unknown, ApiError, SetStengtHosArrangorRequest>({
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
