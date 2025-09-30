import { useQueryClient } from "@tanstack/react-query";
import { AvtaleDetaljerRequest, AvtalerService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { AvtaleDto, ProblemDetail } from "@tiltaksadministrasjon/api-client";

export function useUpsertAvtaleDetaljer(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, AvtaleDetaljerRequest>({
    // TODO: fjern any når denne flyttes til nytt api-endepunkt
    mutationFn: (body: AvtaleDetaljerRequest) =>
      AvtalerService.upsertAvtaleDetaljer({
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
