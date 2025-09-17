import { useQueryClient } from "@tanstack/react-query";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSlettOpsjon(avtaleId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: (opsjonId: string) =>
      AvtaleService.slettOpsjon({
        path: { id: avtaleId, opsjonId },
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
