import { useQueryClient } from "@tanstack/react-query";
import { GjennomforingService, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useGodkjennGjennomforingOkonomi() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => GjennomforingService.godkjennGjennomforingOkonomi({ path: { id } }),
    async onSuccess(_, { id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.gjennomforing(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.gjennomforingHandlinger(id) }),
      ]);
    },
  });
}
