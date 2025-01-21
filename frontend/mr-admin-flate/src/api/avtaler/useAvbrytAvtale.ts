import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AvbrytAvtaleAarsak, AvtalerService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytAvtale() {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (data: { id: string; aarsak?: AvbrytAvtaleAarsak | string }) => {
      return AvtalerService.avbrytAvtale({
        path: { id: data.id },
        body: { aarsak: data.aarsak },
      });
    },
    onSuccess(_, request) {
      return Promise.all([
        client.invalidateQueries({
          queryKey: QueryKeys.avtale(request.id),
        }),
        client.invalidateQueries({
          queryKey: QueryKeys.avtaler(),
        }),
      ]);
    },
  });
}
