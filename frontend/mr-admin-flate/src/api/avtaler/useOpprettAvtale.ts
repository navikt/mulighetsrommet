import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  AvtaleService,
  OpprettAvtaleRequest,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";

export function useOpprettAvtale() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, OpprettAvtaleRequest>({
    mutationFn: async (body: OpprettAvtaleRequest) => {
      return AvtaleService.opprettAvtale({ body });
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.avtale(request.id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.avtaler() }),
      ]);
    },
  });
}
