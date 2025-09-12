import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingService, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSetTilgjengeligForArrangor() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, any>({
    mutationFn: async ({ id, dato }: { id: string; dato: string }) => {
      return GjennomforingService.setTilgjengeligForArrangor({
        path: { id: id },
        body: { tilgjengeligForArrangorDato: dato },
      });
    },

    onSuccess(_, request) {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
        queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(request.id),
        }),
      ]);
    },
  });
}
