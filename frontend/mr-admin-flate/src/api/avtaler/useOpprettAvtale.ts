import { useQueryClient } from "@tanstack/react-query";
import { AvtaleRequest, AvtalerService, ProblemDetail } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";

export function useOpprettAvtale() {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, AvtaleRequest>({
    mutationFn: (body: AvtaleRequest) => {
      // TODO: fjern any når denne flyttes til nytt api-endepunkt
      return AvtalerService.opprettAvtale({ body }) as any;
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
