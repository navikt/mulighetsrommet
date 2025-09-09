import { useQueryClient } from "@tanstack/react-query";
import { AvtaleService, OpprettOpsjonLoggRequest } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useRegistrerOpsjon(avtaleId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: (body: OpprettOpsjonLoggRequest) =>
      AvtaleService.registrerOpsjon({
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
