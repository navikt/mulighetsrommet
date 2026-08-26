import { useQueryClient } from "@tanstack/react-query";
import { EnkeltplassService, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSettOkonomiPaVent() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; forklaring: string | null; totrinnskontrollId: string }
  >({
    mutationFn: ({ id, forklaring, totrinnskontrollId }) => {
      return EnkeltplassService.settOkonomiPaVent({
        path: { id },
        body: { forklaring, totrinnskontrollId },
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
