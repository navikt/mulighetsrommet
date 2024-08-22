import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, AvbrytAvtaleAarsak, AvtalerService } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useAvbrytAvtale() {
  const client = useQueryClient();

  return useMutation<unknown, ApiError, { id: string; aarsak: AvbrytAvtaleAarsak | string }>({
    mutationFn: (data: { id: string; aarsak?: AvbrytAvtaleAarsak | string }) => {
      return AvtalerService.avbrytAvtale({
        id: data.id,
        requestBody: { aarsak: data.aarsak },
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
