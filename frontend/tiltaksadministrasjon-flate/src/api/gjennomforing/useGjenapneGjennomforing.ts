import { useQueryClient } from "@tanstack/react-query";
import { GjennomforingService, ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useGjenapneGjennomforing() {
  const client = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, { id: string; nySluttDato: string }>({
    mutationFn: (data) => {
      return GjennomforingService.gjenapneGjennomforing({
        path: { id: data.id },
        body: { nySluttDato: data.nySluttDato },
      });
    },
    onSuccess(_, request) {
      return Promise.all([
        client.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(request.id),
        }),
        client.invalidateQueries({
          queryKey: QueryKeys.gjennomforinger(),
        }),
      ]);
    },
  });
}
