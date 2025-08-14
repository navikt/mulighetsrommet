import { useQueryClient } from "@tanstack/react-query";
import { AvbrytAvtaleAarsak, AvtalerService, ProblemDetail } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useAvbrytAvtale() {
  const client = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; aarsaker: AvbrytAvtaleAarsak[]; forklaring: string | null }
  >({
    mutationFn: (data: {
      id: string;
      aarsaker: AvbrytAvtaleAarsak[];
      forklaring: string | null;
    }) => {
      return AvtalerService.avbrytAvtale({
        path: { id: data.id },
        body: { aarsaker: data.aarsaker, forklaring: data.forklaring },
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
