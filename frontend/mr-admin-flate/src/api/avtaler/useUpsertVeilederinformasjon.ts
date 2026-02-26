import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  AvtaleDto,
  AvtaleService,
  ProblemDetail,
  VeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";

export function useUpsertVeilederinformasjon(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, VeilederinfoRequest>({
    mutationFn: (body: VeilederinfoRequest) =>
      AvtaleService.upsertVeilederinfo({
        path: { id },
        body,
      }),
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtale(id),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtaler(),
        }),
      ]);
    },
  });
}
