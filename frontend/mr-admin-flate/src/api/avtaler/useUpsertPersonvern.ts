import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  AvtaleDto,
  AvtaleService,
  PersonvernRequest,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";

export function useUpsertPersonvern(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, PersonvernRequest>({
    mutationFn: (body: PersonvernRequest) =>
      AvtaleService.upsertPersonvern({
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
