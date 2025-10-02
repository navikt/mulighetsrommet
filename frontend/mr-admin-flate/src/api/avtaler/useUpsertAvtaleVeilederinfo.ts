import { useQueryClient } from "@tanstack/react-query";
import { AvtaleVeilederinfoRequest, AvtalerService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { AvtaleDto, ProblemDetail } from "@tiltaksadministrasjon/api-client";

export function useUpsertAvtaleVeilederinfo(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, AvtaleVeilederinfoRequest>({
    // TODO: fjern any når denne flyttes til nytt api-endepunkt
    mutationFn: (body: AvtaleVeilederinfoRequest) =>
      AvtalerService.upsertAvtaleVeilederinfo({
        path: { id },
        body,
      }) as any,
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtale(id),
        }),
      ]);
    },
  });
}
