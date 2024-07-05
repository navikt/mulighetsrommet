import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, OpsjonerService, SlettOpsjonLoggRequest } from "mulighetsrommet-api-client";
import { QueryKeys } from "../QueryKeys";

export function useSlettOpsjon() {
  const queryClient = useQueryClient();

  return useMutation<string, ApiError, SlettOpsjonLoggRequest>({
    mutationFn: (requestBody) => OpsjonerService.slettOpsjon({ requestBody }),
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
