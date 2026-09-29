import { useQueryClient } from "@tanstack/react-query";
import {
  AvbrytAvtaleAarsak,
  AvtaleService,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useAvbrytAvtale() {
  const client = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; aarsaker: AvbrytAvtaleAarsak[]; begrunnelse: string | null }
  >({
    mutationFn: (data: {
      id: string;
      aarsaker: AvbrytAvtaleAarsak[];
      begrunnelse: string | null;
    }) => {
      return AvtaleService.avbrytAvtale({
        path: { id: data.id },
        body: { aarsaker: data.aarsaker, begrunnelse: data.begrunnelse },
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
