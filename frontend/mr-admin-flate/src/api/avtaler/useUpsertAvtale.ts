import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, Avtale, AvtaleRequest } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useUpsertAvtale() {
  const queryClient = useQueryClient();

  return useMutation<Avtale, ApiError, AvtaleRequest>({
    mutationFn: (requestBody: AvtaleRequest) =>
      mulighetsrommetClient.avtaler.upsertAvtale({ requestBody }),

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
