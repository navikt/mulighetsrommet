import { useQueryClient } from "@tanstack/react-query";
import { AvtaleService, RammedetaljerRequest } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertRammedetaljer(avtaleId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: (body: RammedetaljerRequest) =>
      AvtaleService.upsertRammedetaljer({
        path: { id: avtaleId },
        body,
      }),
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtale(avtaleId),
        }),
      ]);
    },
  });
}
