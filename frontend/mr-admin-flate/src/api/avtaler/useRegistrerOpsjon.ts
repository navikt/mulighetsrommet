import { useQueryClient } from "@tanstack/react-query";
import { OpsjonerService, OpsjonLoggRequest } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useRegistrerOpsjon() {
  const queryClient = useQueryClient();

  return useApiMutation({
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
