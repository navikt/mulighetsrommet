import { useQueryClient } from "@tanstack/react-query";
import {
  AvbrytGjennomforingAarsak,
  GjennomforingService,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useAvbrytGjennomforing() {
  const client = useQueryClient();

  return useApiMutation<
    unknown,
    ProblemDetail,
    {
      id: string;
      aarsaker: AvbrytGjennomforingAarsak[];
      forklaring: string | null;
      dato: string | null;
    }
  >({
    mutationFn: (data: {
      id: string;
      aarsaker: AvbrytGjennomforingAarsak[];
      forklaring: string | null;
      dato: string | null;
    }) => {
      return GjennomforingService.avbrytGjennomforing({
        path: { id: data.id },
        body: {
          aarsakerOgForklaringRequest: { aarsaker: data.aarsaker, forklaring: data.forklaring },
          dato: data.dato,
        },
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
