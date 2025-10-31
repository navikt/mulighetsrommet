import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { AvtaleDto, AvtaleRequest, AvtaleService, ProblemDetail } from "@tiltaksadministrasjon/api-client";

export function useUpsertAvtale() {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, AvtaleRequest>({
    mutationFn: async (body: AvtaleRequest) => {
      return AvtaleService.upsertAvtale({ body });
    },

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
