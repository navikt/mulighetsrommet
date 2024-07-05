import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, OpsjonerService, OpsjonLoggRequest } from "mulighetsrommet-api-client";
import { QueryKeys } from "../QueryKeys";

export function useRegistrerOpsjon() {
  const queryClient = useQueryClient();

  return useMutation<String, ApiError, OpsjonLoggRequest>({
    mutationFn: (requestBody: OpsjonLoggRequest) => OpsjonerService.lagreOpsjon({ requestBody }),
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
