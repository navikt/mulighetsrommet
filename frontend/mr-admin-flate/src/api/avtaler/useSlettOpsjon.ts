import { useMutation, useQueryClient } from "@tanstack/react-query";
import { OpsjonerService, SlettOpsjonLoggRequest } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

export function useSlettOpsjon() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: SlettOpsjonLoggRequest) => OpsjonerService.slettOpsjon({ body }),
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
