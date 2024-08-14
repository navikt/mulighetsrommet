import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, OpsjonerService, OpsjonLoggRequest } from "@mr/api-client";
import { QueryKeys } from "../QueryKeys";

export function useRegistrerOpsjon() {
  const queryClient = useQueryClient();

  return useMutation<string, ApiError, OpsjonLoggRequest>({
    mutationFn: (requestBody) => OpsjonerService.lagreOpsjon({ requestBody }),
    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtale(request.avtaleId),
        }),

        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtaler(),
        }),
      ]);
    },
  });
}
