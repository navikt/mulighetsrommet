import { useMutation, useQueryClient } from "@tanstack/react-query";
import { OpsjonerService, OpsjonLoggRequest } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

export function useRegistrerOpsjon() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: OpsjonLoggRequest) => OpsjonerService.lagreOpsjon({ body }),
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
