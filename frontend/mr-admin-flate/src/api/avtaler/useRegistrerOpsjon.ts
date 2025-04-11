import { useQueryClient } from "@tanstack/react-query";
import { OpsjonerService, OpsjonLoggRequest } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useRegistrerOpsjon(avtaleId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: (body: OpsjonLoggRequest) =>
      OpsjonerService.lagreOpsjon({
        path: { id: avtaleId },
        body,
      }),
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtale(avtaleId),
        }),

        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtaler(),
        }),
      ]);
    },
  });
}
