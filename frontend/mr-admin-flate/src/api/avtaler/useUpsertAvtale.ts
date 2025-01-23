import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AvtaleRequest, AvtalerService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertAvtale() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: AvtaleRequest) => AvtalerService.upsertAvtale({ body }),

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtale(request.id),
        }),

        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtaler(),
        }),
      ]);
    },
  });
}
