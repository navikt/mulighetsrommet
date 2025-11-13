import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  AvtaleDto,
  AvtaleService,
  DetaljerRequest,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";

export function useUpsertDetaljer(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, DetaljerRequest>({
    mutationFn: (body: DetaljerRequest) =>
      AvtaleService.upsertDetaljer({
        path: { id },
        body,
      }),
    onSuccess() {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.avtale(id),
        }),
      ]);
    },
  });
}
