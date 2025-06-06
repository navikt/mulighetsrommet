import { useQueryClient } from "@tanstack/react-query";
import { OpsjonerService, SlettOpsjonLoggRequest } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSlettOpsjon(avtaleId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: (body: SlettOpsjonLoggRequest) =>
      OpsjonerService.slettOpsjon({
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
