import { useQueryClient } from "@tanstack/react-query";
import {
  GjennomforingDetaljerRequest,
  GjennomforingService,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpdateGjennomforingDetaljer(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, GjennomforingDetaljerRequest>({
    mutationFn: async (body: GjennomforingDetaljerRequest) => {
      return GjennomforingService.updateGjennomforingDetaljer({ path: { id }, body });
    },

    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.gjennomforing(id),
      });
    },
  });
}
