import { useQueryClient } from "@tanstack/react-query";
import { EnkeltplassService, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSettOkonomiPaVent() {
  const queryClient = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; begrunnelse: string | null; totrinnskontrollId: string }
  >({
    mutationFn: ({ id, begrunnelse, totrinnskontrollId }) => {
      return EnkeltplassService.settOkonomiPaVent({
        path: { id },
        body: { begrunnelse, totrinnskontrollId },
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
