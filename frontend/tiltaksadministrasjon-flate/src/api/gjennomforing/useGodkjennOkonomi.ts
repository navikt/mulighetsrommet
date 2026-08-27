import { useQueryClient } from "@tanstack/react-query";
import { EnkeltplassService, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useGodkjennOkonomi() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string; totrinnskontrollId: string }>({
    mutationFn: ({ id, totrinnskontrollId }) => {
      return EnkeltplassService.godkjennOkonomi({
        path: { id },
        body: { totrinnskontrollId },
      });
    },
    async onSuccess(_, { id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.gjennomforing(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.gjennomforingHandlinger(id) }),
      ]);
    },
  });
}
