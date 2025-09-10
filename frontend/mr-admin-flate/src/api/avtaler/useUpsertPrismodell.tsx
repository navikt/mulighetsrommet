import { useQueryClient } from "@tanstack/react-query";
import {
  AvtaleDto,
  AvtaleService,
  PrismodellRequest,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertPrismodell(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<{ data: AvtaleDto }, ProblemDetail, PrismodellRequest>({
    mutationFn: (body: PrismodellRequest) =>
      AvtaleService.upsertPrismodell({
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
