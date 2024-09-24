import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, AvtaleDto, AvtaleRequest, AvtalerService } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertAvtale() {
  const queryClient = useQueryClient();

  return useMutation<AvtaleDto, ApiError, AvtaleRequest>({
    mutationFn: (requestBody: AvtaleRequest) => AvtalerService.upsertAvtale({ requestBody }),

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
